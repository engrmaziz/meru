# Meru Support Playbook (Summit)

## Channels
- Play Console replies (48h SLA soft launch)
- Email: support placeholder `support@meru.app` (configure DNS later)

## Common issues
| Symptom | Check | Fix |
|---------|-------|-----|
| Trip not syncing | Token, network, WorkManager | Re-open app on Wi‑Fi; check `/health` |
| Booking failed | `bookingsEnabled` flag, slot hold | Admin metrics; ask user to retry |
| Missing certified stamp | Invoice not confirmed | Owner must Confirm in Stamp |
| Staff can't see history | Share expired | Owner re-book / new share |
| Rank missing | Integrity / opt-out | Privacy toggle; integrity ≥ competitive min |

## Admin basics
```bash
# Flags / kill switches
curl -H "X-Admin-Key: $MERU_ADMIN_KEY" http://HOST/v1/admin/flags
curl -X PATCH -H "X-Admin-Key: $MERU_ADMIN_KEY" -H "Content-Type: application/json" \
  -d '{"bookingsEnabled":false}' http://HOST/v1/admin/flags

# Metrics
curl -H "X-Admin-Key: $MERU_ADMIN_KEY" http://HOST/v1/admin/metrics

# User lookup
curl -X POST -H "X-Admin-Key: $MERU_ADMIN_KEY" -H "Content-Type: application/json" \
  -d '{"email":"driver@example.com"}' http://HOST/v1/admin/support/lookup
```

## Account deletion
Owner: More → Delete account → `DELETE /v1/account` (vault purge).  
Document in Play Data Safety.

## Escalation
Integrity disputes / invoice disputes → log userId + invoiceId; never hand-edit competitive scores client-side.
