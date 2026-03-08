declare global {
  namespace Cypress {
    interface Chainable {
      login(email: string, password: string): Chainable<void>;
      registerAndLogin(username: string, email: string, password: string): Chainable<void>;
    }
  }
}

/**
 * Log in via the UI (navigates to /login, fills credentials, submits).
 */
Cypress.Commands.add('login', (email: string, password: string) => {
  cy.visit('/login');
  cy.get('input[name="email"], input[type="email"]').first().type(email);
  cy.get('input[name="password"], input[type="password"]').first().type(password);
  cy.get('button[type="submit"]').click();
  cy.url().should('include', '/dashboard');
});

/**
 * Register a new user then immediately log in.
 */
Cypress.Commands.add('registerAndLogin', (username: string, email: string, password: string) => {
  cy.visit('/register');
  cy.get('input[name="username"], input[formcontrolname="username"]').first().type(username);
  cy.get('input[name="email"], input[formcontrolname="email"], input[type="email"]').first().type(email);
  cy.get('input[name="password"], input[formcontrolname="password"], input[type="password"]').first().type(password);
  cy.get('button[type="submit"]').click();
  cy.url().should('include', '/login');
  cy.login(email, password);
});

export {};
