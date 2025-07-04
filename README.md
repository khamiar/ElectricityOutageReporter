# ElectricityOutageReporter

POST /api/outages - Create new report
PUT /api/outages/{id}/status - Update report status
GET /api/outages/summary - Get status summary
GET /api/outages/my - Get user's reports
GET /api/outages - Get all reports
GET /api/outages/{id} - Get specific report
GET /api/outages/status/{status} - Get reports by status
DELETE /api/outages/{id} - Delete report
GET /api/outages/generate - Generate report (PDF/Excel)