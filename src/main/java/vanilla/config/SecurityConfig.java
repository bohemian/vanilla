package vanilla.config;

/**
 * Authentication and authorisation are intentionally not implemented here.
 *
 * <p>All inbound traffic is validated by an Istio {@code RequestAuthentication} sidecar before it
 * reaches this service. The sidecar verifies the JWT signature against the AWS Cognito JWKS
 * endpoint and rejects unauthenticated requests at the network layer ({@code AuthorizationPolicy:
 * DENY} rule).
 *
 * <p>Decoded claims (e.g. {@code sub}, {@code cognito:groups}) are forwarded as trusted HTTP
 * headers ({@code X-User-Id}, {@code X-User-Groups}) by the sidecar. Controllers may read these
 * headers directly; they must never trust the same headers supplied by the caller, as the sidecar
 * strips them before forwarding.
 *
 * <p>Do not add Spring Security JWT filter logic here — doing so would duplicate responsibility
 * that belongs to the platform team, and would diverge from the organisation-wide auth standard.
 */
public final class SecurityConfig {
  private SecurityConfig() {}
}
