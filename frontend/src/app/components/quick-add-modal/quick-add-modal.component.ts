import { Component, HostListener, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { QuickAddService } from '../../services/quick-add.service';
import { TipService } from '../../services/tip.service';

@Component({
  selector: 'app-quick-add-modal',
  templateUrl: './quick-add-modal.component.html',
  styleUrls: ['./quick-add-modal.component.scss']
})
export class QuickAddModalComponent implements OnInit {
  form: FormGroup;
  submitting = false;
  success = false;
  errorMsg = '';

  private readonly defaultStartTimes: Record<string, string> = {
    Morning: '08:00',
    Evening: '15:45',
    Night:   '18:00',
  };

  constructor(
    private fb: FormBuilder,
    public quickAdd: QuickAddService,
    private tipService: TipService
  ) {
    this.form = this.fb.group({
      cashTips:    ['', [Validators.required, Validators.min(0)]],
      creditTips:  ['', [Validators.required, Validators.min(0)]],
      date:        [new Date().toISOString().split('T')[0], Validators.required],
      shiftType:   ['', Validators.required],
      startTime:   [''],
      endTime:     [''],
      notes:       [''],
      peopleInPool:['1', [Validators.required, Validators.min(1)]],
    });
  }

  ngOnInit(): void {
    this.quickAdd.isOpen$.subscribe(open => {
      if (open) this.resetForm();
    });
  }

  get totalTips(): number {
    const cash   = parseFloat(this.form.get('cashTips')?.value)   || 0;
    const credit = parseFloat(this.form.get('creditTips')?.value) || 0;
    return cash + credit;
  }

  get hoursWorked(): number | null {
    const start = this.form.get('startTime')?.value;
    const end   = this.form.get('endTime')?.value;
    if (!start || !end) return null;
    const [sh, sm] = start.split(':').map(Number);
    const [eh, em] = end.split(':').map(Number);
    const minutes = (eh * 60 + em) - (sh * 60 + sm);
    return minutes > 0 ? Math.round((minutes / 60) * 100) / 100 : null;
  }

  selectShift(shift: string): void {
    this.form.get('shiftType')?.setValue(shift);
    const saved = localStorage.getItem(`shiftStart_${shift}`);
    this.form.get('startTime')?.setValue(saved || this.defaultStartTimes[shift]);
  }

  onSubmit(): void {
    if (this.form.invalid || this.submitting) return;
    this.submitting = true;
    this.errorMsg = '';
    this.tipService.addTip(this.form.value).subscribe({
      next: () => {
        const shift = this.form.get('shiftType')?.value;
        const start = this.form.get('startTime')?.value;
        if (shift && start) localStorage.setItem(`shiftStart_${shift}`, start);
        this.submitting = false;
        this.success = true;
        setTimeout(() => this.close(), 1500);
      },
      error: () => {
        this.submitting = false;
        this.errorMsg = 'Submission failed. Please try again.';
      }
    });
  }

  close(): void {
    this.quickAdd.close();
    this.success = false;
    this.errorMsg = '';
  }

  onBackdropClick(e: MouseEvent): void {
    if ((e.target as HTMLElement).classList.contains('quick-add-backdrop')) {
      this.close();
    }
  }

  @HostListener('document:keydown.escape')
  onEsc(): void { this.close(); }

  private resetForm(): void {
    this.form.reset({
      date: new Date().toISOString().split('T')[0],
      startTime: '', endTime: '',
      peopleInPool: '1',
    });
    this.success = false;
    this.errorMsg = '';
    this.submitting = false;
  }
}
