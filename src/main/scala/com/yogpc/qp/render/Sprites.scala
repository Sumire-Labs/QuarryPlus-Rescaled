package com.yogpc.qp.render

import com.yogpc.qp.QuarryPlus
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.TextureStitchEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.mutable

object Sprites {

  val instance = this
  private val map = mutable.Map.empty[Symbol, TextureAtlasSprite]

  def getMap = map.toMap

  @SubscribeEvent
  def registerTexture(event: TextureStitchEvent.Pre): Unit = {
    val textureMap = event.getMap
    LaserType.values().foreach(laserType => map.put(laserType.symbol, textureMap.registerSprite(laserType.location())))
    val put_F = (name: Symbol) => map.put(name, textureMap.registerSprite(new ResourceLocation(QuarryPlus.modID, "entities/" + name.name)))
    List(
      Symbol("laser_1"),
      Symbol("laser_2"),
      Symbol("laser_3"),
      Symbol("laser_4"),
      Symbol("yellow"),
      Symbol("stripes_h"),
      Symbol("stripes_v"),
      Symbol("stripes_b"),
      Symbol("stripes_refinery")).foreach(put_F)
  }
}
