# Issues List

## 1. Webhook Trust Boundary Airwallex
### Background
Establishing a trust boundary for Airwallex webhook to ensure secure handling of notifications.
### Goals
- Define trust boundary criteria.
- Implement validation checks on endpoint.
### Scope
Focus on the Airwallex integration and webhook handling.
### API/Routes
- `/webhook/airwallex`
### Data model
- Define payload structure expected from Airwallex.
### Security notes
- Validate incoming requests.
- Implement logging for suspicious activities.
### Definition of Done
- Code is written, reviewed, and tested.
- Documentation is updated.
### References
- [Airwallex Documentation](https://www.airwallex.com/docs)

---

## 2. Webhook Trust Boundary Swan
### Background
Implementing security measures for Swan webhooks.
### Goals
- Define trust boundary.
- Secure the webhook route.
### Scope
Swan integration and webhook processing.
### API/Routes
- `/webhook/swan`
### Data model
- Confirm payload structure from Swan.
### Security notes
- Authentication for requests.
### Definition of Done
- Successful deployment and monitoring.
### References
- [Swan Documentation](https://swan.com/docs)

---

## 3. financial_webhook_events schema + status machine
### Background
Creating a unified schema for financial webhook events.
### Goals
- Standardize the event types and statuses.
### Scope
All financial APIs.
### API/Routes
No new routes required. Refactor existing ones.
### Data model
- Event types: success, failure, pending.
### Security notes
- Ensure data integrity.
### Definition of Done
- Schema approved and implemented.
### References
- Internal documentation.

---

## 4. gateway_parameters value-structure fix and helper
### Background
Correcting value structures for gateway parameters to avoid conflicts.
### Goals
- Ensure consistency in parameter values.
### Scope
Gateway parameter handlers.
### API/Routes
- Update existing gateway parameter routes.
### Data model
- Parameter structures documented.
### Security notes
- Validate input parameters.
### Definition of Done
- Tests passed and code reviewed.
### References
- Gateway documentation and code.

---

## 5. Alipay IPN/query 500 stability and observability
### Background
Enhancing stability for Alipay Instant Payment Notifications.
### Goals
- Reduce errors and improve observability.
### Scope
Alipay related services.
### API/Routes
- `/ipn/alipay/query`
### Data model
- Log structures for IPN.
### Security notes
- Monitor for suspicious activity.
### Definition of Done
- Metrics collected and functionality tested.
### References
- [Alipay Documentation](https://www.alipay.com/docs)

---

## 6. Protect high-risk endpoints /clear /cron /ipn/*/query
### Background
Securing high-risk API endpoints against attacks.
### Goals
- Implement additional security layers.
### Scope
Endpoints particularly vulnerable to misuse.
### API/Routes
- `/clear`, `/cron`, `/ipn/*/query`
### Data model
- Logs for access and attempts.
### Security notes
- Rate limiting and IP whitelisting.
### Definition of Done
- Security measures implemented and tested.
### References
- Security best practices documentation.

---

## 7. Admin forced 2FA
### Background
Enforcement of two-factor authentication for admin access.
### Goals
- Strengthen admin account security.
### Scope
Admin interface.
### API/Routes
- `/admin/login`
### Data model
- User 2FA settings.
### Security notes
- Ensure 2FA can’t be bypassed.
### Definition of Done
- 2FA enforced and user notified.
### References
- 2FA implementation guidelines.

---

## 8. Auth provider plugin framework in Admin Extensions
### Background
Developing a framework for adding auth providers in Admin extensions.
### Goals
- Flexibility in adding new auth methods.
### Scope
Admin extensions module.
### API/Routes
- `/admin/extensions/auth`
### Data model
- Auth provider configurations.
### Security notes
- Check against common vulnerabilities.
### Definition of Done
- Framework designed and documented.
### References
- Plugin development documentation.

---

## 9. Intl OAuth providers Google/Facebook/Apple/TikTok
### Background
Implementing OAuth for international providers.
### Goals
- Expand authentication options for users.
### Scope
User authentication modules.
### API/Routes
- `/oauth/initiate/google`, `/oauth/initiate/facebook`, etc.
### Data model
- OAuth provider configurations.
### Security notes
- Secure token storage.
### Definition of Done
- OAuth implemented and tested.
### References
- Provider documentation.

---

## 10. China OAuth providers WeChat/Alipay login
### Background
Adding support for WeChat and Alipay login methods.
### Goals
- Offer local payment and auth methods.
### Scope
User interface and API.
### API/Routes
- `/oauth/initiate/wechat`, `/oauth/initiate/alipay`
### Data model
- OAuth configuration for Chinese providers.
### Security notes
- Compliance with local regulations.
### Definition of Done
- Integration completed and functioning correctly.
### References
- WeChat and Alipay documentation.

---

## 11. OTP channel plugin WhatsApp/TG/SMS/Email
### Background
Creating a plugin framework for OTP channels.
### Goals
- Provide multiple channels for OTP.
### Scope
User authentication processes.
### API/Routes
- `/otp/send`
### Data model
- OTP channel configurations.
### Security notes
- Ensure OTPs are short-lived.
### Definition of Done
- Plugin is operational and documented.
### References
- OTP best practices documentation.

---

## 12. Multi-tenant entry adaption: Web login pages + API /auth/providers and /oauth routes for user/agent/merchant
### Background
Adapting entry points for multi-tenant use.
### Goals
- Support various entry methods for tenants.
### Scope
User, agent, and merchant login processes.
### API/Routes
- `/auth/providers`, `/oauth`
### Data model
- Tenant-specific configurations.
### Security notes
- Validate user context in API calls.
### Definition of Done
- Multi-tenant functionality validated.
### References
- Multi-tenancy guidelines.

---

## 13. Documentation system and website footer 'Official Docs' button to /docs
### Background
Improving documentation accessibility through the website.
### Goals
- Make documentation easy to find.
### Scope
Website frontend.
### API/Routes
- `/docs`
### Data model
- Footer configuration.
### Security notes
- Ensure links are secured.
### Definition of Done
- Button added and links functioning.
### References
- Web design best practices.