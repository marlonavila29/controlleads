import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

function withToken(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
}

/**
 * Attaches the Bearer token and, on a 401 from a protected endpoint, tries a
 * silent refresh once and replays the request with the new token. A failed
 * refresh logs out (handled inside AuthService.refresh).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  // Never touch the auth endpoints themselves (login/refresh/forgot/reset).
  if (req.url.startsWith('/api/auth/')) {
    return next(req);
  }

  return next(withToken(req, auth.accessToken)).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) {
        return throwError(() => error);
      }
      return auth.refresh().pipe(
        switchMap(() => next(withToken(req, auth.accessToken))),
        catchError((refreshErr) => throwError(() => refreshErr))
      );
    })
  );
};
