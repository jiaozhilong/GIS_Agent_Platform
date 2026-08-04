import { use } from 'echarts/core'
import { BarChart, GraphChart, LineChart, PieChart, RadarChart } from 'echarts/charts'
import { DatasetComponent, GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([CanvasRenderer, BarChart, GraphChart, LineChart, PieChart, RadarChart, DatasetComponent, GridComponent, LegendComponent, TitleComponent, TooltipComponent])
