export class ApiError extends Error {
  readonly fieldErrors: Readonly<Record<string, string>>

  constructor(
    message: string,
    readonly status: number,
    fieldErrors: Record<string, string> = {},
  ) {
    super(message)
    this.name = 'ApiError'
    this.fieldErrors = fieldErrors
  }
}
