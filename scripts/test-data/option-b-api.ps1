param(
    [string]$BaseUrl = 'http://localhost:8084',
    [string]$Email = 'integration@fluxis.local'
)

$ErrorActionPreference = 'Stop'
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrfToken = $null
$csrfHeader = $null

function Invoke-Api {
    param(
        [ValidateSet('GET', 'POST', 'PUT', 'DELETE')]
        [string]$Method,
        [string]$Path,
        [object]$Body
    )

    $headers = @{}
    if ($csrfHeader -and $Method -ne 'GET') {
        $headers[$csrfHeader] = $csrfToken
    }

    $request = @{
        Uri = "$BaseUrl$Path"
        Method = $Method
        WebSession = $session
        Headers = $headers
        ErrorAction = 'Stop'
    }

    if ($null -ne $Body) {
        $request.ContentType = 'application/json'
        $request.Body = $Body | ConvertTo-Json -Depth 10 -Compress
    }

    return Invoke-RestMethod @request
}

function Get-PageItems {
    param([string]$Path)

    $firstPage = Invoke-Api -Method GET -Path "${Path}?page=0&size=100"
    $items = @($firstPage.content)
    $totalPages = [int]$firstPage.totalPages

    for ($page = 1; $page -lt $totalPages; $page++) {
        $nextPage = Invoke-Api -Method GET -Path "${Path}?page=$page&size=100"
        $items += @($nextPage.content)
    }

    return $items
}

function Ensure-Entity {
    param(
        [string]$Path,
        [string]$Property,
        [string]$Value,
        [object]$Body,
        [string]$Label
    )

    $existing = @(Get-PageItems -Path $Path | Where-Object { [string]$_.$Property -eq $Value } | Select-Object -First 1)
    if ($existing.Count -gt 0) {
        Write-Host "Reused ${Label}: $Value"
        return $existing[0]
    }

    $created = Invoke-Api -Method POST -Path $Path -Body $Body
    Write-Host "Created ${Label}: $Value"
    return $created
}

$passwordSecure = Read-Host 'Password for the integration user' -AsSecureString
$passwordPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($passwordSecure)
try {
    $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPtr)
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPtr)
}

$csrf = Invoke-Api -Method GET -Path '/api/auth/csrf'
$csrfToken = $csrf.token
$csrfHeader = $csrf.headerName
Invoke-Api -Method POST -Path '/api/auth/login' -Body @{ email = $Email; password = $password } | Out-Null
$password = $null

$company = Ensure-Entity -Path '/api/companies' -Property 'name' -Value 'Option B Integration Company' -Label 'company' -Body @{
    name = 'Option B Integration Company'
    website = 'https://option-b.example.test'
    industry = 'Technology Services'
    notes = 'Synthetic API dataset for integration verification.'
}

$contact = Ensure-Entity -Path '/api/contacts' -Property 'email' -Value 'option-b.contact@example.test' -Label 'contact' -Body @{
    companyId = $company.id
    firstName = 'Option'
    lastName = 'Contact'
    email = 'option-b.contact@example.test'
    jobTitle = 'Technology Director'
    origin = 'Option B API demo'
    notes = 'Synthetic contact for integration verification.'
}

$client = Ensure-Entity -Path '/api/clients' -Property 'displayName' -Value 'Option B Integration Client' -Label 'client' -Body @{
    companyId = $company.id
    contactId = $contact.id
    displayName = 'Option B Integration Client'
    status = 'POTENCIAL'
    origin = 'Option B API demo'
    notes = 'Synthetic client for integration verification.'
}

$service = Ensure-Entity -Path '/api/services' -Property 'name' -Value 'Option B Platform Review' -Label 'service' -Body @{
    name = 'Option B Platform Review'
    description = 'Synthetic professional service created through the API.'
    category = 'Architecture'
    pricingModel = 'FIXED'
    rate = 1250.00
    currency = 'EUR'
    available = $true
    status = 'ACTIVE'
    notes = 'Synthetic service for integration verification.'
}

$proposal = Ensure-Entity -Path '/api/proposals' -Property 'title' -Value 'Option B Integration Proposal' -Label 'proposal' -Body @{
    clientId = $client.id
    title = 'Option B Integration Proposal'
    description = 'Synthetic proposal created through the API.'
    currency = 'EUR'
    validUntil = '2026-12-31'
    status = 'SENT'
    items = @(@{
        serviceId = $service.id
        description = 'Platform review'
        quantity = 1
        unitPrice = 1250.00
    })
}

$subscription = Ensure-Entity -Path '/api/subscriptions' -Property 'name' -Value 'Option B Monitoring Subscription' -Label 'subscription' -Body @{
    name = 'Option B Monitoring Subscription'
    provider = 'Option B Provider'
    amount = 49.90
    currency = 'EUR'
    periodicity = 'MONTHLY'
    nextChargeDate = '2026-12-01'
    renewalDate = '2027-01-01'
    status = 'ACTIVE'
    category = 'Monitoring'
    notes = 'Synthetic subscription for integration verification.'
    managementUrl = 'https://option-b.example.test/subscription'
}

$task = Ensure-Entity -Path '/api/tasks' -Property 'title' -Value 'Review Option B proposal' -Label 'task' -Body @{
    title = 'Review Option B proposal'
    description = 'Synthetic task linked to the Option B client.'
    dueDate = '2026-12-05'
    priority = 'HIGH'
    status = 'TODO'
    origin = 'Option B API demo'
    relatedEntityType = 'CLIENT'
    relatedEntityId = $client.id
}

$opportunity = Ensure-Entity -Path '/api/job-opportunities' -Property 'url' -Value 'https://jobs.example.test/option-b/api-engineer' -Label 'job opportunity' -Body @{
    companyName = 'Option B Systems'
    title = 'API Integration Engineer'
    url = 'https://jobs.example.test/option-b/api-engineer'
    source = 'Option B API demo'
    postedOn = '2026-11-10'
    description = 'Synthetic opportunity created through the authenticated API.'
    requirements = 'Synthetic integration requirements.'
    technologies = @('Java', 'Spring Boot', 'PostgreSQL')
    status = 'INTERESTING'
}

$applications = @(Get-PageItems -Path '/api/job-applications')
$application = $applications | Where-Object { [string]$_.jobOpportunityId -eq [string]$opportunity.id } | Select-Object -First 1
if ($null -eq $application) {
    $application = Invoke-Api -Method POST -Path '/api/job-applications' -Body @{
        jobOpportunityId = $opportunity.id
        status = 'APPLICATION_SENT'
        appliedOn = '2026-11-12'
        notes = 'Synthetic application created through the authenticated API.'
    }
    Write-Host 'Created job application'
}
else {
    Write-Host 'Reused job application'
}

Write-Host ''
Write-Host 'Option B API dataset is ready.'
Write-Host "Company: $($company.id)"
Write-Host "Client: $($client.id)"
Write-Host "Proposal: $($proposal.id)"
Write-Host "Subscription: $($subscription.id)"
Write-Host "Task: $($task.id)"
Write-Host "Opportunity: $($opportunity.id)"
Write-Host "Application: $($application.id)"
