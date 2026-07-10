import axios from 'axios';
import { Request, Response, NextFunction } from 'express';
import qs from 'qs';

export const keycloakAuth = async (req: Request, res: Response, next: NextFunction) => {
  const username = req.headers['x-username'] as string;
  // Express lowercases incoming header names, so only 'authtoken' ever matches.
  const token = req.headers['authtoken'] as string;

  const KEYCLOAK_URL = process.env.KEYCLOAK_URL || 'http://keycloak:8080';
  const REALM = process.env.KEYCLOAK_REALM || 'teastore';
  const CLIENT_ID = process.env.KEYCLOAK_CLIENT_ID || 'admin-cli';
  const CLIENT_SECRET = process.env.KEYCLOAK_CLIENT_SECRET || 'secret';

  console.log(' Auth Debug - Headers:', { username, tokenProvided: !!token, tokenLength: token?.length });

  if (!username) return res.status(401).json({ message: 'Username header missing' });
  if (!token) return res.status(401).json({ message: 'Token header missing' });

  try {
    console.log(' Getting Keycloak client token...');
    const tokenResponse = await axios.post(
      `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`,
      qs.stringify({ client_id: CLIENT_ID, client_secret: CLIENT_SECRET, grant_type: 'client_credentials' }),
      { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
    );

    const accessToken = tokenResponse.data.access_token;
    console.log(' Client token obtained');

    console.log(' Searching for user:', username);
    const userResponse = await axios.get(
      `${KEYCLOAK_URL}/admin/realms/${REALM}/users`,
      { params: { username, exact: true }, headers: { Authorization: `Bearer ${accessToken}` } }
    );

    const users = userResponse.data;
    if (!users || users.length === 0) return res.status(403).json({ message: 'User not found' });

    const user = users[0];
    console.log(' User details:', { id: user.id, username: user.username, enabled: user.enabled });

    // Normalize tokens
    const tokenTrimmed = token.trim();
    const isLoggedInAttr = user.attributes?.authToken;

    let tokens: string[] = [];
    if (isLoggedInAttr) {
      if (Array.isArray(isLoggedInAttr)) tokens = isLoggedInAttr.map(t => String(t).trim());
      else if (typeof isLoggedInAttr === 'object') tokens = Object.values(isLoggedInAttr).map(t => String(t).trim());
      else tokens = [String(isLoggedInAttr).trim()];
    }

    // Case-insensitive check
    const isAuthenticated = tokens.some(t => t.toLowerCase() === tokenTrimmed.toLowerCase());
    console.log(' Auth check result:', isAuthenticated);

    if (isAuthenticated) {
      console.log('Authentication successful');
      return next();
    } else {
      console.log('Authentication failed - token mismatch or not found');
      return res.status(403).json({ message: 'User not logged in' });
    }

  } catch (err: any) {
    console.error(' Keycloak error:', err.response?.data || err.message);
    return res.status(500).json({ message: 'Error checking user in Keycloak' });
  }
};

