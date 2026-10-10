param(
    [int[]] $Sizes = @(1024, 512, 256, 32, 16)
)

$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
$textureDirectory = Join-Path $projectDirectory 'src/main/resources/assets/quarryplus/textures/blocks'

# Reconstruct the cube from models/block/quarryplus.json with orthographic
# projection (45-degree horizontal rotation, 30-degree elevation).
# Original texture pixels are sampled with nearest-neighbor filtering.
# Only face brightness changes; no generated details or replacement textures.
Add-Type -AssemblyName System.Drawing
if (-not ('QuarryIconRenderer' -as [type])) {
    $drawingReferences = @('System.Drawing.Common', 'System.Drawing.Primitives')
    # Newer PowerShell/.NET versions split GDI+ interfaces into private assemblies.
    foreach ($name in @('System.Private.Windows.GdiPlus', 'System.Private.Windows.Core')) {
        $assemblyPath = Join-Path $PSHOME "$name.dll"
        if (Test-Path -LiteralPath $assemblyPath) { $drawingReferences += $assemblyPath }
    }
    Add-Type -ReferencedAssemblies $drawingReferences -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class QuarryIconRenderer
{
    public static void AddRescaledLabel(string inputPath, string outputPath)
    {
        // A tiny bitmap font avoids external fonts and stays crisp at icon size.
        string[] glyphs = {
            "11110/10001/10001/11110/10100/10010/10001", // R
            "00000/00000/01110/10001/11111/10000/01110", // e
            "00000/00000/01111/10000/01110/00001/11110", // s
            "00000/00000/01110/10000/10000/10001/01110", // c
            "00000/00000/01110/00001/01111/10001/01111", // a
            "01100/00100/00100/00100/00100/00100/01110", // l
            "00000/00000/01110/10001/11111/10000/01110", // e
            "00001/00001/01111/10001/10001/10001/01111"  // d
        };
        using (var image = new Bitmap(inputPath))
        {
            int scale = Math.Max(1, image.Width / 51);
            int margin = Math.Max(3, image.Width / 32);
            int textWidth = (glyphs.Length * 6 - 1) * scale;
            int originX = image.Width - margin - textWidth;
            int originY = margin;
            bool[,] mask = new bool[image.Width, image.Height];
            for (int letter = 0; letter < glyphs.Length; letter++)
            {
                string[] rows = glyphs[letter].Split('/');
                for (int y = 0; y < 7; y++)
                for (int x = 0; x < 5; x++)
                if (rows[y][x] == '1')
                for (int dy = 0; dy < scale; dy++)
                for (int dx = 0; dx < scale; dx++)
                    mask[originX + (letter * 6 + x) * scale + dx, originY + y * scale + dy] = true;
            }
            // One-pixel dark outline keeps the red letters readable on either UI theme.
            for (int y = 1; y < image.Height - 1; y++)
            for (int x = 1; x < image.Width - 1; x++)
            if (mask[x, y])
            for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++)
                if (!mask[x + dx, y + dy]) image.SetPixel(x + dx, y + dy, Color.FromArgb(255, 42, 12, 12));
            for (int y = 0; y < image.Height; y++)
            for (int x = 0; x < image.Width; x++)
                if (mask[x, y]) image.SetPixel(x, y, Color.FromArgb(255, 238, 40, 40));
            image.Save(outputPath, ImageFormat.Png);
        }
    }

    private struct Point
    {
        public double X, Y;
        public Point(double x, double y) { X = x; Y = y; }
    }

    private sealed class Face
    {
        public Bitmap Texture;
        public Point Origin, U, V;
        public double Shade;
        public Face(Bitmap texture, Point origin, Point u, Point v, double shade)
        {
            Texture = texture; Origin = origin; U = u; V = v; Shade = shade;
        }

        public bool Sample(double x, double y, out Color color)
        {
            double dx = x - Origin.X, dy = y - Origin.Y;
            double determinant = U.X * V.Y - U.Y * V.X;
            double u = (dx * V.Y - dy * V.X) / determinant;
            double v = (U.X * dy - U.Y * dx) / determinant;
            if (u < 0 || u >= 1 || v < 0 || v >= 1)
            {
                color = Color.Transparent;
                return false;
            }
            Color original = Texture.GetPixel(
                Math.Min((int)(u * Texture.Width), Texture.Width - 1),
                Math.Min((int)(v * Texture.Height), Texture.Height - 1));
            color = Color.FromArgb(original.A,
                (int)Math.Round(original.R * Shade),
                (int)Math.Round(original.G * Shade),
                (int)Math.Round(original.B * Shade));
            return true;
        }
    }

    public static void Render(string topPath, string frontPath, string sidePath, string outputPath, int size)
    {
        if (size < 16 || size > 4096) throw new ArgumentOutOfRangeException("size");
        using (var top = new Bitmap(topPath))
        using (var front = new Bitmap(frontPath))
        using (var side = new Bitmap(sidePath))
        using (var output = new Bitmap(size, size, PixelFormat.Format32bppArgb))
        {
            double elevation = Math.PI / 6;
            double halfWidth = Math.Sqrt(0.5);
            double halfDepth = halfWidth * Math.Sin(elevation);
            double height = Math.Cos(elevation);
            double scale = size * 0.88 / Math.Max(2 * halfWidth, 2 * halfDepth + height);
            double a = halfWidth * scale, b = halfDepth * scale, c = height * scale;
            double centerX = size / 2.0;
            double topY = (size - 2 * b - c) / 2;

            Face[] faces = {
                new Face(top, new Point(centerX, topY), new Point(a, b), new Point(-a, b), 1.0),
                new Face(front, new Point(centerX - a, topY + b), new Point(a, b), new Point(0, c), 0.8),
                new Face(side, new Point(centerX, topY + 2 * b), new Point(a, -b), new Point(0, c), 0.6)
            };

            for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
            {
                Color color = Color.Transparent;
                foreach (Face face in faces)
                {
                    if (face.Sample(x + 0.5, y + 0.5, out color)) break;
                }
                output.SetPixel(x, y, color);
            }
            output.Save(outputPath, ImageFormat.Png);
        }
    }
}
'@
}

foreach ($size in $Sizes) {
    $outputPath = Join-Path $PSScriptRoot "quarry-icon-$size.png"
    [QuarryIconRenderer]::Render(
        (Join-Path $textureDirectory 'quarry_top.png'),
        (Join-Path $textureDirectory 'quarry_front.png'),
        (Join-Path $textureDirectory 'quarry.png'),
        $outputPath,
        $size
    )
    Write-Output $outputPath
}
