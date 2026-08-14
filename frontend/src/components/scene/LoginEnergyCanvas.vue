<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as THREE from 'three'

const host = ref<HTMLDivElement | null>(null)
let renderer: THREE.WebGLRenderer | undefined
let animation = 0
let resizeObserver: ResizeObserver | undefined
let removePointerListener: (() => void) | undefined
let disposeScene: (() => void) | undefined

onMounted(() => {
  if (!host.value) return

  const scene = new THREE.Scene()
  const camera = new THREE.PerspectiveCamera(46, 1, 0.1, 100)
  camera.position.set(0, 1.1, 8.4)
  renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true, powerPreference: 'high-performance' })
  renderer.setClearColor(0x000000, 0)
  renderer.setPixelRatio(Math.min(devicePixelRatio, 1.7))
  host.value.appendChild(renderer.domElement)

  const energy = new THREE.Group()
  // Align the live energy center with the focal tower in the approved city artwork.
  energy.position.set(0.1, 0.65, 0)
  scene.add(energy)

  const additive = THREE.AdditiveBlending
  const coreMaterial = new THREE.MeshBasicMaterial({
    color: 0x8ff7ff,
    wireframe: true,
    transparent: true,
    opacity: 0.7,
    blending: additive,
    depthWrite: false,
  })
  const core = new THREE.Mesh(new THREE.IcosahedronGeometry(0.24, 2), coreMaterial)
  core.position.y = 0.08
  energy.add(core)

  const innerCore = new THREE.Mesh(
    new THREE.SphereGeometry(0.13, 24, 16),
    new THREE.MeshBasicMaterial({
      color: 0x1ba7ff,
      transparent: true,
      opacity: 0.72,
      blending: additive,
      depthWrite: false,
    }),
  )
  innerCore.position.copy(core.position)
  energy.add(innerCore)

  const shell = new THREE.Mesh(
    new THREE.SphereGeometry(0.46, 30, 20),
    new THREE.MeshBasicMaterial({
      color: 0x168cff,
      wireframe: true,
      transparent: true,
      opacity: 0.11,
      blending: additive,
      depthWrite: false,
    }),
  )
  shell.position.copy(core.position)
  energy.add(shell)

  const rings: THREE.Mesh[] = []
  ;[0.72, 1.06, 1.43].forEach((radius, index) => {
    const ring = new THREE.Mesh(
      new THREE.TorusGeometry(radius, index === 1 ? 0.018 : 0.012, 8, 128),
      new THREE.MeshBasicMaterial({
        color: index === 1 ? 0x1689ff : 0x43edff,
        transparent: true,
        opacity: 0.62 - index * 0.1,
        blending: additive,
        depthWrite: false,
      }),
    )
    ring.rotation.x = Math.PI / 2 + index * 0.22
    ring.position.copy(core.position)
    energy.add(ring)
    rings.push(ring)
  })

  const beamMaterials: THREE.MeshBasicMaterial[] = []
  const beamBaseOpacities = [0.2, 0.038, 0.014]
  const createBeam = (radius: number, height: number, opacity: number) => {
    const material = new THREE.MeshBasicMaterial({
      color: 0x2dbdff,
      transparent: true,
      opacity,
      side: THREE.DoubleSide,
      blending: additive,
      depthWrite: false,
    })
    const beam = new THREE.Mesh(new THREE.CylinderGeometry(radius * 0.25, radius, height, 30, 1, true), material)
    beam.position.y = height / 2 + 0.1
    energy.add(beam)
    beamMaterials.push(material)
  }
  createBeam(0.1, 5.2, 0.2)
  createBeam(0.34, 4.8, 0.038)
  createBeam(0.72, 4.1, 0.014)

  const scans: Array<{ mesh: THREE.Mesh; material: THREE.MeshBasicMaterial; offset: number }> = []
  for (let index = 0; index < 4; index += 1) {
    const material = new THREE.MeshBasicMaterial({
      color: 0x32dfff,
      transparent: true,
      opacity: 0,
      blending: additive,
      depthWrite: false,
    })
    const mesh = new THREE.Mesh(new THREE.TorusGeometry(0.84, 0.014, 6, 128), material)
    mesh.rotation.x = Math.PI / 2
    mesh.position.y = -0.3
    energy.add(mesh)
    scans.push({ mesh, material, offset: index / 4 })
  }

  const particleCount = 420
  const particlePositions = new Float32Array(particleCount * 3)
  const particleSpeeds = new Float32Array(particleCount)
  for (let index = 0; index < particleCount; index += 1) {
    const stride = index * 3
    const angle = Math.random() * Math.PI * 2
    const radius = 0.45 + Math.random() * 3.4
    particlePositions[stride] = Math.cos(angle) * radius
    particlePositions[stride + 1] = -0.35 + Math.random() * 4.4
    particlePositions[stride + 2] = Math.sin(angle) * radius * 0.52
    particleSpeeds[index] = 0.18 + Math.random() * 0.48
  }
  const particleGeometry = new THREE.BufferGeometry()
  particleGeometry.setAttribute('position', new THREE.BufferAttribute(particlePositions, 3))
  const particleMaterial = new THREE.PointsMaterial({
    color: 0x62e9ff,
    size: 0.035,
    transparent: true,
    opacity: 0.82,
    blending: additive,
    depthWrite: false,
  })
  const particles = new THREE.Points(particleGeometry, particleMaterial)
  energy.add(particles)

  const ambientCount = 150
  const ambientPositions = new Float32Array(ambientCount * 3)
  for (let index = 0; index < ambientPositions.length; index += 3) {
    ambientPositions[index] = (Math.random() - 0.5) * 15
    ambientPositions[index + 1] = (Math.random() - 0.5) * 8
    ambientPositions[index + 2] = (Math.random() - 0.5) * 5
  }
  const ambientGeometry = new THREE.BufferGeometry()
  ambientGeometry.setAttribute('position', new THREE.BufferAttribute(ambientPositions, 3))
  const ambientMaterial = new THREE.PointsMaterial({
    color: 0x2aa8ff,
    size: 0.026,
    transparent: true,
    opacity: 0.55,
    blending: additive,
    depthWrite: false,
  })
  const ambient = new THREE.Points(ambientGeometry, ambientMaterial)
  scene.add(ambient)

  const pointer = new THREE.Vector2()
  const pointerTarget = new THREE.Vector2()
  const onPointerMove = (event: PointerEvent) => {
    pointerTarget.set(event.clientX / window.innerWidth - 0.5, event.clientY / window.innerHeight - 0.5)
  }
  window.addEventListener('pointermove', onPointerMove, { passive: true })
  removePointerListener = () => window.removeEventListener('pointermove', onPointerMove)

  const resize = () => {
    if (!host.value || !renderer) return
    const { clientWidth: width, clientHeight: height } = host.value
    renderer.setSize(width, height, false)
    camera.aspect = width / Math.max(height, 1)
    camera.updateProjectionMatrix()
  }
  resizeObserver = new ResizeObserver(resize)
  resizeObserver.observe(host.value)
  resize()

  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const clock = new THREE.Clock()
  const tick = () => {
    const delta = Math.min(clock.getDelta(), 0.04)
    const elapsed = clock.elapsedTime
    pointer.lerp(pointerTarget, 0.035)

    if (!reduceMotion) {
      const pulse = 1 + Math.sin(elapsed * 3.2) * 0.09
      core.rotation.x = elapsed * 0.42
      core.rotation.y = elapsed * 0.68
      core.scale.setScalar(pulse)
      innerCore.scale.setScalar(0.92 + Math.sin(elapsed * 4.2) * 0.14)
      shell.rotation.y = -elapsed * 0.14
      shell.scale.setScalar(1 + Math.sin(elapsed * 1.6) * 0.045)
      rings.forEach((ring, index) => {
        ring.rotation.z = elapsed * (0.34 + index * 0.09) * (index % 2 ? -1 : 1)
      })
      beamMaterials.forEach((material, index) => {
        material.opacity = beamBaseOpacities[index] * (0.82 + Math.sin(elapsed * 2.2 + index) * 0.18)
      })
      scans.forEach(({ mesh, material, offset }) => {
        const phase = (elapsed * 0.23 + offset) % 1
        mesh.scale.setScalar(0.5 + phase * 1.5)
        material.opacity = Math.sin(phase * Math.PI) * 0.42
      })
      for (let index = 0; index < particleCount; index += 1) {
        const yIndex = index * 3 + 1
        particlePositions[yIndex] += particleSpeeds[index] * delta
        if (particlePositions[yIndex] > 4.2) particlePositions[yIndex] = -0.35
      }
      particleGeometry.attributes.position.needsUpdate = true
      particles.rotation.y = elapsed * 0.025
      ambient.rotation.y = elapsed * 0.012
      energy.rotation.y = pointer.x * 0.08
      energy.rotation.x = pointer.y * 0.025
      camera.position.x = pointer.x * 0.16
      camera.position.y = 1.1 - pointer.y * 0.1
    }

    renderer?.render(scene, camera)
    animation = requestAnimationFrame(tick)
  }
  animation = requestAnimationFrame(tick)

  disposeScene = () => {
    scene.traverse((object) => {
      if (object instanceof THREE.Mesh || object instanceof THREE.Points) {
        object.geometry.dispose()
        const materials = Array.isArray(object.material) ? object.material : [object.material]
        materials.forEach((material) => material.dispose())
      }
    })
  }
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animation)
  resizeObserver?.disconnect()
  removePointerListener?.()
  disposeScene?.()
  renderer?.dispose()
  renderer?.domElement.remove()
})
</script>

<template>
  <div ref="host" class="energy-canvas" aria-hidden="true" />
</template>

<style scoped>
.energy-canvas {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  mix-blend-mode: screen;
  filter: saturate(1.16) drop-shadow(0 0 16px rgba(35, 178, 255, 0.22));
}
</style>
