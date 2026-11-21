export class ApiError extends Error {
  readonly fieldErrors: Readonly<Record<string, string>>
  readonly url: string
  readonly method: string
  readonly responseBody: string

  constructor(
    message: string,
    readonly status: number,
    fieldErrors: Record<string, string> = {},
    url = '',
    method = 'GET',
    responseBody = '',
  ) {
    super(message)
    this.name = 'ApiError'
    this.fieldErrors = fieldErrors
    this.url = url
    this.method = method
    this.responseBody = responseBody.slice(0, 1000)
  }
}

export class ResponseValidationError extends ApiError {
  constructor(message: string, url: string, method: string, responseBody: string) {
    super(message, 200, {}, url, method, responseBody)
    this.name = 'ResponseValidationError'
  }
}
