import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SlideToggleService {

  areAllDataSelected(dataList: any): boolean {
      for (const element of dataList) {
        for (const dataItem of element.data) {
          if (!dataItem.selected) {
            return false;
          }
        }
      }
      return true;
  }

  isAtLeastOneSelected(dataList: any): boolean {
    for (const element of dataList) {
        for (const dataItem of element.data) {
            if (dataItem.selected) {
                return true;
            }
        }
    }
    return false;
  }

  onChange(dataList: any, $event: any, toggleName: string, dataType: any, data: any): boolean {
    const isChecked = $event.checked;
    if (toggleName == 'all') {
      dataList.forEach((item: any) => {
        item.answerByData = isChecked;
        item.data.forEach((dataItem: any) => {
          dataItem.answerByData = isChecked;
        });
      });
    }
    else if (toggleName == 'dataType') {
      dataType.answerByData = isChecked;

      dataType.data.forEach((dataItem: any) => {
        dataItem.answerByData = isChecked;
      });

      if (isChecked && dataType.data.some((item: any) => item.answerByData)) {
        dataType.answerByData = true;
      } else {
        dataType.answerByData = false;
      }
    }
    else {
      data.answerByData = isChecked;
      const dataType = dataList.find((item: any) => item.data.some((d: any) => d === data));
      if (dataType) {
        dataType.answerByData = dataType.data.some((item: any) => item.answerByData);
      }
    }
    return this.areAllDataSelected(dataList);
  }
}
