import { Component, input } from '@angular/core';
import { label } from './catalog';
import { record, text } from './admin-utils';
@Component({selector:'app-record-view',template:`<dl class="admin-details">@for(item of entries();track item[0]) {<div><dt>{{label(item[0])}}</dt><dd>@if(isObject(item[1])) {<div class="nested"><app-record-view [value]="item[1]" /></div>} @else if(isArray(item[1])) { @for(child of array(item[1]);track $index) {<div class="nested">@if(isObject(child)){<app-record-view [value]="child"/>}@else{{{text(child)}}}</div>} @empty {<span>Sin registros</span>} } @else {{{text(item[1])}}}</dd></div>}</dl>`})
export class RecordView {
 readonly value=input<unknown>();readonly label=label;readonly text=text;
 entries(){return Object.entries(record(this.value())).map(([key,value]):[string,unknown]=>{if((key==='antes'||key==='despues')&&typeof value==='string'){try{return [key,JSON.parse(value) as unknown];}catch{/* Historical audit may contain descriptive text. */}}return [key,value];});}
 isObject(value:unknown){return value!==null&&typeof value==='object'&&!Array.isArray(value);}
 isArray=Array.isArray;
 array(value:unknown):unknown[]{return Array.isArray(value)?value:[];}
}
