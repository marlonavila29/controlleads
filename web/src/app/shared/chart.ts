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

/**
 * Thin ECharts wrapper — no ngx-echarts dependency, one less version to chase.
 * Charts animate on load/update using the durations from the design tokens
 * (pass them inside the option; see analytics helpers).
 */
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

/** Reads a design token CSS variable at runtime — charts stay on-brand. */
export function cssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

export function chartPalette(): string[] {
  return [1, 2, 3, 4, 5, 6, 7, 8].map((i) => cssVar(`--cl-color-chart-${i}`));
}

/** Base option shared by every chart: animation timing + font from tokens. */
export function baseChartOption(): EChartsOption {
  return {
    color: chartPalette(),
    animationDuration: 800,
    animationEasing: 'cubicOut',
    textStyle: { fontFamily: cssVar('--cl-font-family-base') },
    tooltip: { trigger: 'item' }
  };
}
