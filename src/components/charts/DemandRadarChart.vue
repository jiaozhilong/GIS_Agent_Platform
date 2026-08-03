<script setup lang="ts">
import VChart from 'vue-echarts'
const option = {
  radar: { indicator: [{ name: '数据治理', max: 100 }, { name: '平台能力', max: 100 }, { name: '业务应用', max: 100 }, { name: '智能分析', max: 100 }, { name: '开放集成', max: 100 }], radius: '67%', axisName: { color: '#9bb0c1' }, splitLine: { lineStyle: { color: 'rgba(86,147,190,.2)' } }, splitArea: { areaStyle: { color: ['rgba(9,29,45,.12)', 'rgba(6,21,34,.28)'] } }, axisLine: { lineStyle: { color: 'rgba(86,147,190,.22)' } } },
  series: [{ type: 'radar', data: [{ value: [92, 76, 81, 66, 70], name: '需求强度', areaStyle: { color: 'rgba(27,125,255,.27)' }, lineStyle: { color: '#32a9ff', width: 2 }, itemStyle: { color: '#49e4ff' } }] }]
}
</script>
<template><VChart :option="option" autoresize /></template>
