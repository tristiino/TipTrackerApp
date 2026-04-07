/**
 * Mirrors the backend TipOutType enum.
 * PERCENTAGE: deduction = grossTips * (amount / 100)
 * FIXED_AMOUNT: deduction = amount (flat dollar)
 */
export type TipOutType = 'PERCENTAGE' | 'FIXED_AMOUNT';

/**
 * Which tip pool this role draws from.
 * CASH   — deduction based on cash tips only
 * CREDIT — deduction based on credit tips only
 * BOTH   — deduction based on combined total (default)
 */
export type TipOutSource = 'CASH' | 'CREDIT' | 'BOTH';

/**
 * Mirrors the backend TipOutRoleDTO.
 * Represents a reusable tip-out role template owned by the user.
 */
export interface TipOutRole {
  id?: number;
  name: string;
  splitType: TipOutType;
  amount: number;
  source?: TipOutSource;
  /** null/undefined = global role (all jobs); set = scoped to that job */
  jobId?: number;
  jobName?: string;
}

/**
 * Mirrors the backend TipOutRecordDTO.
 * Represents the actual deduction applied to a specific shift.
 *
 * computedAmount: what the formula produced at time of entry
 * finalAmount:    what actually gets deducted (may differ if overridden)
 * isOverridden:   true if the user manually changed finalAmount
 */
export interface TipOutRecord {
  id?: number;
  roleId?: number;
  roleName: string;
  computedAmount: number;
  finalAmount: number;
  isOverridden: boolean;
}
