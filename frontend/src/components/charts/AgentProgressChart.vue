<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
const props = defineProps<{ requirementCount?:number; proposalCount?:number; runningCount?:number; documentCount?:number }>()
const option = computed(()=>({grid:{left:80,right:24,top:8,bottom:8},xAxis:{type:'value',show:false},yAxis:{type:'category',inverse:true,data:['需求分析','方案生成','运行 Agent','知识文档'],axisLine:{show:false},axisTick:{show:false},axisLabel:{color:'#9bb0c1',fontSize:10}},series:[{type:'bar',barWidth:7,showBackground:true,backgroundStyle:{color:'rgba(118,160,190,.1)'},itemStyle:{color:'#258dff',borderRadius:4},data:[props.requirementCount||0,props.proposalCount||0,props.runningCount||0,props.documentCount||0],label:{show:true,position:'right',color:'#8fa7b9',fontSize:9}}]}))
</script>
<template><VChart :option="option" autoresize /></template>
