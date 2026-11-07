# Wattagam backend agent instructions

## Scope and references

- These instructions apply to `backend/`. Follow parent instructions and preserve existing user changes.
- Product reference: [Wattagam MVP PRD v1.2](https://app.notion.com/p/MVP-PRD-v1-2-40f6a540d6648118acaadb91457ed9e7). Coding reference: [team code conventions](https://app.notion.com/p/7d861a0bb8348209999901db199b7c0d).
- This guide summarizes coding conventions and the overall product flow. Keep endpoint paths, request/response formats, error codes and detailed data contracts in separate API specifications.
- Follow explicit current user decisions and the current agreed PRD. Confirm unresolved policies when they affect implementation instead of inferring them from older documents or screens.

## Project overview

- Wattagam (왔다감) is an Android campus-tour app for Chung-Ang University students, alumni and visitors. Its course, 푸른캠퍼스 산책, lets users explore campus, publish photo memories and collect place stamps.
- The initial tour scope is three places: 310관 (75 m visit radius), 도서관 (40 m) and 청룡연못 (30 m). Users may visit them in any order; additional places may be introduced later.
- The backend uses Kotlin, Java 21, Spring Boot, Spring Data JPA, Flyway and MySQL. OCI Object Storage is the planned image store, with deployment planned on OCI.
- `backend/` and `Android/` are independent Gradle projects. Android owns screens, permissions, camera/AR capture and photo decoration. The backend owns authentication, location validation, published records, stamps and image storage.
- The current backend provides the application/database foundation, Kakao authentication, nickname onboarding, place queries, current-location validation, stamp queries and a stamp-granting method for guestbook publication. The flow below describes the intended product, including features still to be implemented.

## Overall product flow

1. Users sign in with Kakao. The backend verifies the Kakao identity, links or creates the account and issues service authentication. New users set a nickname; returning users retain their records and stamps.
2. Signed-in users browse the registered places, their descriptions and public guestbooks. Browsing does not require location permission.
3. Before writing, the backend checks that the user's current GPS satisfies the selected place's visit conditions. Selecting a place or having visited it before does not grant current writing eligibility.
4. Android captures an ordinary or Puang (푸앙이) AR photo, or accepts a gallery image. Users may add drawing/text overlays and an optional message. Android combines the photo and overlays into one final image.
5. On publication, the backend rechecks current location and stores the image and guestbook entry. Publication succeeds only after the required storage work succeeds; failures preserve the draft for a retry with a fresh location check.
6. A successful publication grants at most one stamp per user/place. The total available stamp count follows the registered places, initially three. Entry creation and the first stamp are committed together. Visits, photos, further entries at the same place and retries do not add stamps or duplicate a completed submission.
7. Users browse published entries and view their own records and stamps. All published entries are visible to authenticated app users; unpublished drafts remain outside public galleries and do not earn stamps.
8. Users can manage their own entries and account. Entry edits do not grant stamps; deleting an entry removes its image but preserves its stamp. Account withdrawal removes the account's records, images and stamps according to the agreed deletion policy.

- Draft storage and other unresolved policies remain subject to agreement. Apply the agreed GPS accuracy and freshness limits from the backend configuration; do not introduce server-side draft synchronization or treat provisional values as final requirements.
- Keep authentication, ownership checks, retry safety and image/DB failure recovery on the server. Do not log credentials or precise location, retain movement tracks or introduce background location monitoring.
- Other login providers, guest access, private guestbooks, proximity notifications and image-recognition visit checks are outside this product flow.

## Coding conventions

- Use lowercase package names under the existing `com.wattagam` base. Classes/interfaces use `UpperCamelCase`: classes are nouns; interfaces are nouns or adjectives. Spring bean names use `lowerCamelCase`.
- When these roles exist, suffix controllers, services and repositories with `Controller`, `Service` and `Repository`. Request/response DTO names end in `Request` or `Response`; these naming rules do not require unused layers.
- Functions use descriptive `lowerCamelCase` names starting with a verb or conversion prefix such as `to...`. Variables/properties use descriptive `lowerCamelCase`, preferably nouns. Avoid abbreviations and meaningless single-letter names except short-lived loop/index/lambda variables.
- Give each function one coherent operation and aim for about 20 lines or fewer. This is a readability guideline, not a reason to fragment a clear operation into artificial helpers.
- Constants and enum entries use `UPPER_SNAKE_CASE`; enum type names use `UpperCamelCase`. Use `const val` only for compile-time constants; ordinary immutable properties remain `lowerCamelCase`.
- Use explicit Kotlin imports. The Java source permits wildcard imports only for static members; do not generalize that exception to Kotlin star imports. For Java code, place visibility before `abstract`, `static`, `final` as applicable and write arrays as `String[] names`.
- Use Kotlin declaration syntax and modifier order in Kotlin: `override` instead of Java's `@Override`, `Array<T>`/primitive arrays instead of Java array syntax, and `if` expressions instead of ternary operators. Java overrides require `@Override`.
- Indent new code with four spaces, not tabs. Apply conventions to changed code without unrelated bulk reformatting of existing generated/tab-indented files.
- Use K&R braces: opening brace on the declaration line, closing brace on its own line; `else`, `catch`, `finally` and a do-while's trailing `while` follow the closing brace on the same line. Use braces for conditionals and loops, including single statements.
- Group related declarations. Leave one blank line after the package and import sections, before the first function and between functions; keep related property declarations together. Wrap long lines, especially beyond 120 characters, when it improves readability.
- Put a space after commas and Java for-loop separator semicolons. Follow Kotlin colon syntax (`name: Type`, `class Child : Parent`) rather than copying Java's colon/ternary spacing literally; make operator grouping readable.
- Write code comments in Korean. Explain non-obvious intent, constraints or failure behavior; omit narration of obvious code, stale comments and change-history notes.
- Exceptions raised by application code must have clear Korean messages; custom exception names end in `Exception`. Preserve causes and meaningful failure distinctions without exposing credentials or internal details to clients.
