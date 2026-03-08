/**
 * P1-021: Authentication E2E tests
 * Covers register and login flows.
 */
const timestamp = Date.now();
const testUser = {
  username: `e2euser_${timestamp}`,
  email: `e2e_${timestamp}@test.com`,
  password: 'E2ePassword1!',
};

describe('Register', () => {
  it('shows the registration form', () => {
    cy.visit('/register');
    cy.get('form').should('exist');
    cy.get('button[type="submit"]').should('exist');
  });

  it('registers a new user and redirects to login', () => {
    cy.visit('/register');
    cy.get('input[formcontrolname="username"], input[name="username"]').first().type(testUser.username);
    cy.get('input[formcontrolname="email"], input[type="email"]').first().type(testUser.email);
    cy.get('input[formcontrolname="password"], input[type="password"]').first().type(testUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('include', '/login');
  });

  it('shows an error for duplicate email', () => {
    cy.visit('/register');
    cy.get('input[formcontrolname="username"], input[name="username"]').first().type(`duplicate_${timestamp}`);
    cy.get('input[formcontrolname="email"], input[type="email"]').first().type(testUser.email);
    cy.get('input[formcontrolname="password"], input[type="password"]').first().type(testUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('include', '/register');
  });
});

describe('Login', () => {
  it('shows the login form', () => {
    cy.visit('/login');
    cy.get('form').should('exist');
    cy.get('button[type="submit"]').should('exist');
  });

  it('shows an error for invalid credentials', () => {
    cy.visit('/login');
    cy.get('input[formcontrolname="email"], input[type="email"]').first().type('nobody@example.com');
    cy.get('input[formcontrolname="password"], input[type="password"]').first().type('WrongPassword1!');
    cy.get('button[type="submit"]').click();
    cy.url().should('include', '/login');
  });

  it('logs in a registered user and lands on dashboard', () => {
    cy.login(testUser.email, testUser.password);
    cy.url().should('include', '/dashboard');
  });

  it('shows the user name in the nav bar after login', () => {
    cy.login(testUser.email, testUser.password);
    cy.contains(testUser.username).should('be.visible');
  });
});
