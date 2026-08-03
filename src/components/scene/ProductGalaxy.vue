<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as THREE from 'three'

const host = ref<HTMLDivElement | null>(null)
let renderer: THREE.WebGLRenderer | undefined
let frame = 0
let resizeObserver: ResizeObserver | undefined
const labels = ['GIS Cloud', 'iServer 3D', 'iObjects X', 'iDesktop X', 'iPortal', 'iManager']

onMounted(() => {
  if (!host.value) return
  const scene = new THREE.Scene()
  const camera = new THREE.PerspectiveCamera(45, 1, .1, 100)
  camera.position.set(0, 5.4, 9.2); camera.lookAt(0, 0, 0)
  renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true }); renderer.setPixelRatio(Math.min(devicePixelRatio, 1.5)); host.value.appendChild(renderer.domElement)
  scene.add(new THREE.AmbientLight(0x6ecbff, .7))
  const light = new THREE.PointLight(0x168cff, 20, 14); light.position.set(0, 2, 1); scene.add(light)
  const group = new THREE.Group(); scene.add(group)
  const core = new THREE.Mesh(new THREE.CylinderGeometry(.65, .9, .28, 48), new THREE.MeshStandardMaterial({ color: 0x06366a, emissive: 0x0867d7, emissiveIntensity: 1.2, metalness: .8, roughness: .2 })); group.add(core)
  const orb = new THREE.Mesh(new THREE.SphereGeometry(.28, 32, 32), new THREE.MeshBasicMaterial({ color: 0x4de8ff })); orb.position.y = .55; group.add(orb)
  labels.forEach((_, i) => {
    const angle = i / labels.length * Math.PI * 2
    const platform = new THREE.Mesh(new THREE.BoxGeometry(1.35, .25, 1.05), new THREE.MeshStandardMaterial({ color: i < 3 ? 0x082d58 : 0x392806, emissive: i < 3 ? 0x0b57aa : 0x7f5605, emissiveIntensity: .52, metalness: .72, roughness: .3 }))
    platform.position.set(Math.cos(angle) * 3.15, -.25, Math.sin(angle) * 2.2); platform.rotation.y = -angle + Math.PI / 2; group.add(platform)
    const node = new THREE.Mesh(new THREE.OctahedronGeometry(.22, 0), new THREE.MeshBasicMaterial({ color: i < 3 ? 0x4adfff : 0xffc04a, wireframe: true }))
    node.position.copy(platform.position); node.position.y = .25; group.add(node)
    const points = [new THREE.Vector3(0, .1, 0), new THREE.Vector3(platform.position.x, .1, platform.position.z)]
    const lineGeo = new THREE.BufferGeometry().setFromPoints(points); group.add(new THREE.Line(lineGeo, new THREE.LineBasicMaterial({ color: 0x168cff, transparent: true, opacity: .5 })))
  })
  const resize = () => { if (!host.value || !renderer) return; const { clientWidth: w, clientHeight: h } = host.value; renderer.setSize(w, h, false); camera.aspect = w / Math.max(h, 1); camera.updateProjectionMatrix() }
  resizeObserver = new ResizeObserver(resize); resizeObserver.observe(host.value); resize()
  const tick = (t: number) => { group.rotation.y = Math.sin(t * .00022) * .09; orb.position.y = .55 + Math.sin(t * .002) * .08; renderer?.render(scene, camera); frame = requestAnimationFrame(tick) }
  frame = requestAnimationFrame(tick)
})
onBeforeUnmount(() => { cancelAnimationFrame(frame); resizeObserver?.disconnect(); renderer?.dispose(); renderer?.domElement.remove() })
</script>
<template>
  <div class="galaxy-wrap"><div ref="host" class="galaxy" /><div class="node-label" v-for="(label, i) in labels" :key="label" :style="{ '--i': i }">{{ label }}</div><div class="core-label">GIS<br /><small>能力核心</small></div></div>
</template>
<style scoped>
.galaxy-wrap { position: relative; width: 100%; height: 100%; min-height: 430px; overflow: hidden; background: #020912 url('/assets/images/product-capability-constellation.png') center/contain no-repeat; }
.galaxy { position: absolute; inset: 0; opacity: .22; mix-blend-mode: screen; }
.node-label { position: absolute; left: calc(50% + cos(calc(var(--i) * 60deg)) * 34%); top: calc(51% + sin(calc(var(--i) * 60deg)) * 31%); transform: translate(-50%, -50%); padding: 5px 8px; border: 1px solid rgba(63,154,224,.28); background: rgba(3,14,23,.75); border-radius: 4px; font-size: 11px; color: #c8efff; }
.core-label { position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%); text-align: center; font-weight: 700; color: #eaffff; text-shadow: 0 0 15px #1ac9ff; }.core-label small { color: var(--cyan); font-weight: 500; }
</style>
