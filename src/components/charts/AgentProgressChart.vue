<script setup lang="ts">
import VChart from 'vue-echarts'
const option = {
  grid: { left: 76, right: 18, top: 8, bottom: 8 }, xAxis: { type: 'value', max: 100, show: false },
  yAxis: { type: 'category', inverse: true, data: ['需求分析', '方案生成', 'PPT生成', '知识检索'], axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#9bb0c1', fontSize: 11 } },
  series: [{ type: 'bar', barWidth: 6, showBackground: true, backgroundStyle: { color: 'rgba(118,160,190,.12)' }, itemStyle: { color: '#258dff', borderRadius: 4 }, data: [86, 74, 62, 91], label: { show: true, position: 'right', color: '#8fa7b9', fontSize: 10, formatter: '{c}%' } }]
}
</script>
<template><VChart :option="option" autoresize /></template>
