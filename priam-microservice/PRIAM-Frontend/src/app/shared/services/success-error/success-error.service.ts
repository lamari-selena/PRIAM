import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SuccessErrorService {
  handleSuccess(action: string, response: any) {
    console.log(`[Success] ${action}(): `, response);
  }

  handleError(action: string, error: any) {
    console.error(`[Error] ${action}(): `, error);
  }
}
