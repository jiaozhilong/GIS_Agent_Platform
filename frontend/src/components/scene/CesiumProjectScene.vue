<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { BoxGraphics, Camera, Cartesian2, Cartesian3, Color, ColorMaterialProperty, Entity, Math as CesiumMath, PolylineGlowMaterialProperty, Viewer } from 'cesium'

const host = ref<HTMLDivElement | null>(null)
let viewer: Viewer | undefined
const lon = 116.39, lat = 39.91

const resetCamera = () => {
  if (!viewer || viewer.isDestroyed()) return
  ;(viewer.camera as Camera).setView({ destination: Cartesian3.fromDegrees(lon + .025, lat - .045, 5800), orientation: { heading: CesiumMath.toRadians(340), pitch: CesiumMath.toRadians(-38), roll: 0 } })
}

const setBusinessLayersVisible = (visible: boolean) => {
  if (!viewer || viewer.isDestroyed()) return
  viewer.entities.values.forEach(entity => { entity.show = visible })
}

defineExpose({ resetCamera, setBusinessLayersVisible })

onMounted(() => {
  if (!host.value) return
  viewer = new Viewer(host.value, {
    animation: false, timeline: false, fullscreenButton: false, homeButton: false,
    baseLayerPicker: false, geocoder: false, navigationHelpButton: false,
    sceneModePicker: false, selectionIndicator: false, infoBox: false, baseLayer: false,
    contextOptions: { webgl: { alpha: true } }
  })
  viewer.scene.globe.show = false
  viewer.scene.backgroundColor = Color.TRANSPARENT
  if (viewer.scene.skyBox) viewer.scene.skyBox.show = false
  if (viewer.scene.sun) viewer.scene.sun.show = false
  if (viewer.scene.moon) viewer.scene.moon.show = false
  if (viewer.scene.skyAtmosphere) viewer.scene.skyAtmosphere.show = false
  viewer.scene.fog.enabled = true
  viewer.scene.globe.enableLighting = false
  viewer.scene.globe.showGroundAtmosphere = false
  ;(viewer.cesiumWidget.creditContainer as HTMLElement).style.display = 'none'

  const seed = (n: number) => Math.abs(Math.sin(n * 987.123))
  for (let i = 0; i < 110; i++) {
    const x = (seed(i + 1) - .5) * .075, y = (seed(i + 91) - .5) * .05
    const height = 28 + seed(i + 311) * 160
    const entity = new Entity({
      position: Cartesian3.fromDegrees(lon + x, lat + y, height / 2),
      box: new BoxGraphics({
        dimensions: new Cartesian3(35 + seed(i) * 85, 35 + seed(i + 7) * 80, height),
        material: new ColorMaterialProperty(Color.fromCssColorString(i % 9 === 0 ? '#0a76bbcc' : '#08283ecc')),
        outline: true, outlineColor: Color.fromCssColorString('#25a8e888')
      })
    })
    viewer.entities.add(entity)
  }
  for (let r = -3; r <= 3; r++) {
    viewer.entities.add({ polyline: { positions: Cartesian3.fromDegreesArray([lon - .05, lat + r * .008, lon + .05, lat + r * .008]), width: 2, material: new PolylineGlowMaterialProperty({ color: Color.fromCssColorString('#168cff'), glowPower: .18 }) } })
    viewer.entities.add({ polyline: { positions: Cartesian3.fromDegreesArray([lon + r * .012, lat - .035, lon + r * .012, lat + .035]), width: 1.5, material: new PolylineGlowMaterialProperty({ color: Color.fromCssColorString('#18bfe8'), glowPower: .14 }) } })
  }
  viewer.entities.add({ position: Cartesian3.fromDegrees(lon, lat, 260), point: { pixelSize: 13, color: Color.CYAN, outlineColor: Color.WHITE, outlineWidth: 2 }, label: { text: '项目核心区', fillColor: Color.WHITE, font: '14px Microsoft YaHei', pixelOffset: new Cartesian2(0, -26), showBackground: true, backgroundColor: Color.fromCssColorString('#03101dcc') } })
  resetCamera()
})

onBeforeUnmount(() => { if (viewer && !viewer.isDestroyed()) viewer.destroy() })
</script>
<template><div ref="host" class="cesium-scene" /></template>
<style scoped>
.cesium-scene { width: 100%; height: 100%; min-height: 420px; background: #020912 url('/assets/images/project-war-room-city.png') center/cover no-repeat; }
:deep(.cesium-widget), :deep(.cesium-widget canvas) { background: transparent !important; }
:deep(.cesium-viewer-bottom), :deep(.cesium-viewer-toolbar) { display: none !important; }
</style>
