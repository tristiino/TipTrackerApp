import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TipEntryFormComponent } from './tip-entry-form.component';
import {TipService} from "../../services/tip.service";
import {HttpClientTestingModule} from "@angular/common/http/testing";
import {FormsModule} from "@angular/forms";

describe('TipEntryFormComponent', () => {
  let component: TipEntryFormComponent;
  let fixture: ComponentFixture<TipEntryFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ TipEntryFormComponent ],
      imports: [
        FormsModule,
        HttpClientTestingModule],
      providers: [TipService],
    })
    .compileComponents();

    fixture = TestBed.createComponent(TipEntryFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
