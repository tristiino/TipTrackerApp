import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TipService } from '../../services/tip.service';


@Component({
  selector: 'app-tip-entry-form',
  templateUrl: './tip-entry-form.component.html',
  styleUrls: ['./tip-entry-form.component.scss']
})
export class TipEntryFormComponent implements OnInit {
  tipForm: FormGroup;
  submissionMessage: string | null = null;
  isError: boolean = false;
  recentTips: any[] = [];

  constructor(
    private fb: FormBuilder,
    private tipService: TipService
  ) {
    this.tipForm = this.fb.group({
      amount: ['', [Validators.required, Validators.min(0)]],
      date: ['', [Validators.required]],
      shiftType: ['', [Validators.required]],
      notes: [''],
      peopleInPool: ['', [Validators.required, Validators.min(1)]],
    });
  }

  /**
   * On component initialization, fetches the initial list of recent tips.
   */
  ngOnInit(): void {
    this.loadRecentTips();
  }

  /**
   * Calls the TipService to fetch the most recent tips and updates the component's state.
   */
  loadRecentTips(): void {
    this.tipService.getRecentTips().subscribe({
      next: (tips) => this.recentTips = tips,
      error: (err) => console.error('Failed to load recent tips', err)
    });
  }

  /**
   * Sets the value of the 'shiftType' form control.
   * @param shift The shift type string to set (e.g., 'Morning').
   */
  selectShift(shift: string): void {
    this.tipForm.get('shiftType')?.setValue(shift);
  }

  /**
   * Handles the submission of the tip entry form. If valid, it sends the
   * data to the service and refreshes the recent tips list on success.
   */
  onSubmit(): void {
    if (this.tipForm.valid) {
      this.tipService.addTip(this.tipForm.value).subscribe({
        next: (res: any) => {
          this.isError = false;
          this.submissionMessage = 'Tip submitted successfully!';
          this.tipForm.reset();
          this.loadRecentTips(); // Refresh the list after submitting a new tip
          setTimeout(() => this.submissionMessage = null, 3000);
        },
        error: (err: any) => {
          this.isError = true;
          this.submissionMessage = 'Submission failed. Please try again.';
          setTimeout(() => this.submissionMessage = null, 3000);
        }
      });
    }
  }


  onCancel(): void {
    this.tipForm.reset();
  }
}
