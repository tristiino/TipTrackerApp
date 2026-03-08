/**
 * P1-021: Dashboard E2E tests
 * Verifies the analytics dashboard renders and controls work.
 */
const timestamp = Date.now();
const dashUser = {
  username: `dashuser_${timestamp}`,
  email: `dashuser_${timestamp}@test.com`,
  password: 'DashPass1!',
};

describe('Dashboard', () => {
  before(() => {
    cy.registerAndLogin(dashUser.username, dashUser.email, dashUser.password);
  });

  beforeEach(() => {
    cy.visit('/dashboard');
  });

  it('renders the dashboard page', () => {
    cy.get('.dashboard-container').should('exist');
  });

  it('shows summary stat cards', () => {
    cy.get('.summary-card').should('have.length.gte', 1);
  });

  it('renders a chart canvas', () => {
    cy.get('canvas').should('exist');
  });

  it('has metric toggle buttons', () => {
    cy.get('.metric-toggle button').should('have.length.gte', 2);
  });

  it('has group-by tabs (Daily / Weekly / Monthly)', () => {
    cy.get('.groupby-tabs button').should('have.length', 3);
  });

  it('has time-range tabs', () => {
    cy.get('.time-range-tabs button').should('have.length.gte', 2);
  });

  it('switches metric on button click', () => {
    cy.get('.metric-toggle button').eq(1).click();
    cy.get('.metric-toggle button').eq(1).should('have.class', 'active');
  });

  it('switches to Weekly groupBy', () => {
    cy.get('.groupby-tabs button').eq(1).click();
    cy.get('.groupby-tabs button').eq(1).should('have.class', 'active');
  });

  it('nav bar shows Dashboard link as active', () => {
    cy.get('.navbar-links a.active').should('contain.text', 'Dashboard');
  });

  it('nav bar has Log Tips link', () => {
    cy.get('.navbar-links').contains('Log Tips').should('exist');
  });

  it('nav bar has History link', () => {
    cy.get('.navbar-links').contains('History').should('exist');
  });
});
