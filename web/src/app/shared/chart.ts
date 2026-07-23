import {
  Component,
  ElementRef,
  OnDestroy,
  effect,
  input,
  viewChild
} from '@angular/core';
import * as echarts from 'echarts';
import type { EChartsOption } from 'echarts';

@Component({
  selector: 'app-chart',
  template: `<div #el class="chart" [style.height]="height()"></div>`,
  styles: `
    .chart {
      width: 100%;
    }
  `
})
export class Chart implements OnDestroy {
  readonly options = input.required<EChartsOption>();
  readonly height = input('320px');

  private readonly el = viewChild.required<ElementRef<HTMLDivElement>>('el');

  private chart: echarts.ECharts | null = null;
  private resizeObserver: ResizeObserver | null = null;

  constructor() {
    effect(() => {
      const host = this.el().nativeElement;
      const options = this.options();
      if (!this.chart) {
        this.chart = echarts.init(host);
        this.resizeObserver = new ResizeObserver(() => this.chart?.resize());
        this.resizeObserver.observe(host);
      }
      this.chart.setOption(options);
    });
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.chart?.dispose();
  }
}

export function cssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

export function chartPalette(): string[] {
  return ['#6366F1', '#0EA5E9', '#10B981', '#F59E0B', '#EC4899', '#8B5CF6', '#34D399', '#38BDF8'];
}

export function baseChartOption(): EChartsOption {
  return {
    color: chartPalette(),
    animationDuration: 800,
    animationEasing: 'cubicOut',
    textStyle: { fontFamily: 'Inter, sans-serif', fontSize: 12, color: '#94A3B8' },
    tooltip: {
      trigger: 'item',
      backgroundColor: '#0D1322',
      borderColor: 'rgba(255, 255, 255, 0.12)',
      textStyle: { color: '#F8FAFC', fontSize: 12, fontFamily: 'Inter, sans-serif' },
      borderRadius: 10,
      padding: [10, 14],
      shadowBlur: 16,
      shadowColor: 'rgba(0, 0, 0, 0.5)'
    }
  };
}
