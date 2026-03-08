/**
 * P1-021: Landing page E2E tests
 * Verifies the public home page renders and navigation links work.
 */
describe('Landing page', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  it('shows the app title / brand', () => {
    cy.contains('Tip Tracker').should('be.visible');
  });

  it('has a link to the login page', () => {
    cy.get('a[href*="login"], a[routerlink*="login"]').first().click();
    cy.url().should('include', '/login');
  });

  it('has a link to the register page', () => {
    cy.visit('/');
    cy.get('a[href*="register"], a[routerlink*="register"]').first().click();
    cy.url().should('include', '/register');
  });

  it('redirects unauthenticated users away from /dashboard', () => {
    cy.visit('/dashboard');
    cy.url().should('not.include', '/dashboard');
  });
});
