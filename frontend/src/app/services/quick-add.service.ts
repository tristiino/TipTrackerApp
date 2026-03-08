import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class QuickAddService {
  private _open = new BehaviorSubject<boolean>(false);
  isOpen$ = this._open.asObservable();
  open(): void  { this._open.next(true);  }
  close(): void { this._open.next(false); }
}
