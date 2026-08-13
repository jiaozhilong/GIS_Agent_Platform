<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import type { ProjectStage } from '@/api/contracts'
const props = defineProps<{ counts?: Partial<Record<ProjectStage, number>> }>()
const labels: Record<ProjectStage,string> = { DRAFT:'草稿',REQUIREMENT_ANALYSIS:'需求分析',PRODUCT_MATCH:'产品匹配',KNOWLEDGE_RETRIEVAL:'知识检索',PROPOSAL_GENERATION:'方案生成',REVIEW:'评审',DELIVERED:'已交付' }
const colors = ['#4b6580','#2599ff','#26dcb0','#27e1ff','#9c6dff','#ffc15c','#2ce6a0']
const option = computed(() => ({ backgroundColor:'transparent',tooltip:{trigger:'item'},legend:{right:0,top:'center',orient:'vertical',textStyle:{color:'#8fa7b9',fontSize:9},itemWidth:7,itemHeight:7},series:[{type:'pie',radius:['48%','70%'],center:['34%','52%'],itemStyle:{borderColor:'#07131f',borderWidth:3},label:{show:false},data:Object.entries(props.counts || {}).filter(([,value])=>value>0).map(([stage,value],index)=>({value,name:labels[stage as ProjectStage]||stage,itemStyle:{color:colors[index%colors.length]}}))}] }))
</script>
<template><VChart :option="option" autoresize /></template>
