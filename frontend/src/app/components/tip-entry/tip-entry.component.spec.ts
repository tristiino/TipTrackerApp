import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TipEntryComponent } from './tip-entry.component';

describe('TipEntryComponent', () => {
  let component: TipEntryComponent;
  let fixture: ComponentFixture<TipEntryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ TipEntryComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TipEntryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
