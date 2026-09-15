/** Tokens are `meru_<userId>_<uuid>` from AuthService. */
export function parseBearerUserId(authorization?: string): string | null {
  if (!authorization?.startsWith('Bearer ')) return null;
  const token = authorization.slice('Bearer '.length).trim();
  const parts = token.split('_');
  if (parts.length < 3 || parts[0] !== 'meru') return null;
  return parts[1] || null;
}
