<script setup lang="ts">
import VChart from 'vue-echarts'
const option = {
  backgroundColor: 'transparent',
  tooltip: { trigger: 'item' },
  legend: { right: 0, top: 'center', orient: 'vertical', textStyle: { color: '#8fa7b9', fontSize: 11 }, itemWidth: 9, itemHeight: 9 },
  series: [{ type: 'pie', radius: ['48%', '70%'], center: ['32%', '52%'], avoidLabelOverlap: true, itemStyle: { borderColor: '#07131f', borderWidth: 3 }, label: { show: false }, data: [
    { value: 26, name: '需求分析', itemStyle: { color: '#2599ff' } }, { value: 24, name: '方案编辑', itemStyle: { color: '#9c6dff' } },
    { value: 20, name: '产品匹配', itemStyle: { color: '#26dcb0' } }, { value: 18, name: '项目交付', itemStyle: { color: '#ffc15c' } }, { value: 12, name: '已完成', itemStyle: { color: '#4b6580' } }
  ] }]
}
</script>
<template><VChart :option="option" autoresize /></template>
