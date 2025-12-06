import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TipSummaryComponent } from './tip-summary.component';

describe('TipSummaryComponent', () => {
  let component: TipSummaryComponent;
  let fixture: ComponentFixture<TipSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ TipSummaryComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TipSummaryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
