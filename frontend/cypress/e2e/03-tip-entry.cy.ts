/**
 * P1-021: Tip entry form E2E tests
 * Verifies authenticated users can log a tip.
 */
const timestamp = Date.now();
const tipUser = {
  username: `tipuser_${timestamp}`,
  email: `tipuser_${timestamp}@test.com`,
  password: 'TipPass1!',
};

describe('Tip entry form', () => {
  before(() => {
    cy.registerAndLogin(tipUser.username, tipUser.email, tipUser.password);
  });

  beforeEach(() => {
    cy.visit('/tip-entry-form');
  });

  it('renders the tip entry form', () => {
    cy.get('form').should('exist');
    cy.get('button[type="submit"]').should('be.disabled');
  });

  it('submit button enables when both tip fields have values', () => {
    cy.get('input[formcontrolname="cashTips"]').type('20');
    cy.get('input[formcontrolname="creditTips"]').type('10');
    cy.get('button[type="submit"]').should('not.be.disabled');
  });

  it('shows a live tip total', () => {
    cy.get('input[formcontrolname="cashTips"]').clear().type('25');
    cy.get('input[formcontrolname="creditTips"]').clear().type('15');
    cy.contains('$40.00').should('be.visible');
  });

  it('submits a tip entry and shows success feedback', () => {
    cy.get('input[formcontrolname="cashTips"]').clear().type('30');
    cy.get('input[formcontrolname="creditTips"]').clear().type('20');
    cy.get('button[type="submit"]').click();
    cy.contains(/saved|success|added/i).should('be.visible');
  });

  it('shows a shift type toggle with at least two options', () => {
    cy.get('.shift-buttons button').should('have.length.gte', 2);
  });

  it('can activate a shift type', () => {
    cy.get('.shift-buttons button').first().click();
    cy.get('.shift-buttons button').first().should('have.class', 'active');
  });
});

describe('Quick Add FAB', () => {
  before(() => {
    cy.login(tipUser.email, tipUser.password);
  });

  it('FAB is visible on the dashboard', () => {
    cy.visit('/dashboard');
    cy.get('.fab').should('be.visible');
  });

  it('opens the Quick Add modal when clicked', () => {
    cy.visit('/dashboard');
    cy.get('.fab').click();
    cy.get('.quick-add-modal').should('be.visible');
  });

  it('closes the modal on Cancel', () => {
    cy.visit('/dashboard');
    cy.get('.fab').click();
    cy.get('.btn-cancel').click();
    cy.get('.quick-add-modal').should('not.exist');
  });

  it('closes the modal on ESC key', () => {
    cy.visit('/dashboard');
    cy.get('.fab').click();
    cy.get('.quick-add-modal').should('be.visible');
    cy.get('body').type('{esc}');
    cy.get('.quick-add-modal').should('not.exist');
  });
});
