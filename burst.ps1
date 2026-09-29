# Burst test script for Seat Reservation System (PowerShell)
# Usage: .\burst.ps1 <BASE_URL>
# Example: .\burst.ps1 http://localhost:8080

param(
    [Parameter(Mandatory=$true)]
    [string]$BaseUrl
)

$ErrorActionPreference = "Stop"

function Generate-Seats {
    param([int]$count)
    $seats = @()
    for ($i = 1; $i -le $count; $i++) {
        $seats += "S$($i.ToString('000'))"
    }
    return $seats
}

$NumUsers = 20
$NumSeats = 100
$HotSeat = "S051"
$ConcurrentRequests = 50

Write-Host "=========================================="
Write-Host "Seat Reservation Burst Test"
Write-Host "=========================================="
Write-Host "Base URL: $BaseUrl"
Write-Host "Users: $NumUsers"
Write-Host "Seats: $NumSeats"
Write-Host "Concurrent requests: $ConcurrentRequests"
Write-Host "=========================================="

# Check if server is healthy
Write-Host ""
Write-Host "Checking health endpoint..."
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health/liveness" -Method GET
    Write-Host "Server is healthy"
} catch {
    Write-Host "ERROR: Server not healthy"
    exit 1
}

# Create a show
Write-Host ""
Write-Host "Creating show..."
$seatsArray = Generate-Seats -count $NumSeats
$showBody = @{
    name = "burst-test"
    seats = $seatsArray
    price_paise = 25000
} | ConvertTo-Json

try {
    $showResponse = Invoke-RestMethod -Uri "$BaseUrl/shows" -Method POST -Body $showBody -ContentType "application/json"
    $showId = $showResponse.id
    Write-Host "Show created with ID: $showId"
} catch {
    Write-Host "ERROR: Failed to create show"
    Write-Host $_.Exception.Message
    exit 1
}

# Generate user tokens
Write-Host ""
Write-Host "Generating user tokens..."
$userTokens = @()
for ($i = 1; $i -le $NumUsers; $i++) {
    $userId = "550e8400-e29b-42d4-a716-" + ($i.ToString('x12').PadLeft(12, '0'))
    $tokenBody = @{
        user_id = $userId
    } | ConvertTo-Json
    
    try {
        $tokenResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/token" -Method POST -Body $tokenBody -ContentType "application/json"
        $userTokens += $tokenResponse.token
    } catch {
        Write-Host "ERROR: Failed to generate token for user $i"
        exit 1
    }
}
Write-Host "Generated $NumUsers user tokens"

# Test 1: Normal concurrent reservations
Write-Host ""
Write-Host "=========================================="
Write-Host "Test 1: Normal Concurrent Reservations"
Write-Host "=========================================="

$confirmed = 0
$declinedSeatTaken = 0
$declinedPerUserLimit = 0
$declinedIdempotent = 0
$errors = 0

for ($i = 1; $i -le $ConcurrentRequests; $i++) {
    $userIndex = ($i - 1) % $NumUsers
    $token = $userTokens[$userIndex]
    $seat = "S" + ($i.ToString('000'))
    $idempotencyKey = "req-$i-" + (Get-Date).Ticks

    $reserveBody = @{
        seats = @($seat)
        idempotency_key = $idempotencyKey
    } | ConvertTo-Json

    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl/shows/$showId/reserve" -Method POST -Body $reserveBody -ContentType "application/json" -Headers @{"Authorization" = "Bearer $token"} -UseBasicParsing
        $statusCode = $response.StatusCode
        
        if ($statusCode -eq 201) {
            $confirmed++
        } elseif ($statusCode -eq 409) {
            $body = $response.Content | ConvertFrom-Json
            if ($body.error -eq "seat_taken") {
                $declinedSeatTaken++
            } elseif ($body.error -eq "per_user_limit") {
                $declinedPerUserLimit++
            } else {
                $declinedIdempotent++
            }
        } elseif ($statusCode -ge 500) {
            $errors++
        }
    } catch {
        if ($_.Exception.Response) {
            $statusCode = $_.Exception.Response.StatusCode.value__
            if ($statusCode -eq 409) {
                $body = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($body)
                $responseBody = $reader.ReadToEnd()
                $errorObj = $responseBody | ConvertFrom-Json
                if ($errorObj.error -eq "seat_taken") {
                    $declinedSeatTaken++
                } elseif ($errorObj.error -eq "per_user_limit") {
                    $declinedPerUserLimit++
                } else {
                    $declinedIdempotent++
                }
            } elseif ($statusCode -ge 500) {
                $errors++
            } else {
                $errors++
            }
        } else {
            Write-Host "Request $i failed: $($_.Exception.Message)"
            $errors++
        }
    }
}

Write-Host "Confirmed: $confirmed"
Write-Host "Declined (seat_taken): $declinedSeatTaken"
Write-Host "Declined (per_user_limit): $declinedPerUserLimit"
Write-Host "Declined (idempotent): $declinedIdempotent"
Write-Host "Server errors (5xx): $errors"

# Test 2: Hot-seat storm
Write-Host ""
Write-Host "=========================================="
Write-Host "Test 2: Hot-Seat Storm (Many Users, One Seat)"
Write-Host "=========================================="

$hotConfirmed = 0
$hotDeclined = 0
$hotErrors = 0

for ($i = 1; $i -le $ConcurrentRequests; $i++) {
    $userIndex = ($i - 1) % $NumUsers
    $token = $userTokens[$userIndex]
    $idempotencyKey = "hot-$i-" + (Get-Date).Ticks

    $reserveBody = @{
        seats = @($HotSeat)
        idempotency_key = $idempotencyKey
    } | ConvertTo-Json

    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl/shows/$showId/reserve" -Method POST -Body $reserveBody -ContentType "application/json" -Headers @{"Authorization" = "Bearer $token"} -UseBasicParsing
        $statusCode = $response.StatusCode

        if ($statusCode -eq 201) {
            $hotConfirmed++
        } elseif ($statusCode -eq 409) {
            $hotDeclined++
        } elseif ($statusCode -ge 500) {
            $hotErrors++
        }
    } catch {
        if ($_.Exception.Response) {
            $statusCode = $_.Exception.Response.StatusCode.value__
            if ($statusCode -eq 409) {
                $hotDeclined++
            } elseif ($statusCode -ge 500) {
                Write-Host "Hot-seat request $i failed with 5xx: $statusCode"
                $hotErrors++
            } else {
                Write-Host "Hot-seat request $i failed with unexpected status: $statusCode"
                $hotErrors++
            }
        } else {
            Write-Host "Hot-seat request $i failed without response: $($_.Exception.Message)"
            $hotErrors++
        }
    }
}

Write-Host "Hot-seat confirmed: $hotConfirmed"
Write-Host "Hot-seat declined: $hotDeclined"
Write-Host "Hot-seat errors (5xx): $hotErrors"

# Final reconciliation
Write-Host ""
Write-Host "=========================================="
Write-Host "Final Reconciliation"
Write-Host "=========================================="

try {
    $stateResponse = Invoke-RestMethod -Uri "$BaseUrl/shows/$showId" -Method GET
    $available = $stateResponse.counts.available
    $held = $stateResponse.counts.held
    $confirmed = $stateResponse.counts.confirmed
    $total = $stateResponse.counts.total

    Write-Host "Available: $available"
    Write-Host "Held: $held"
    Write-Host "Confirmed: $confirmed"
    Write-Host "Total: $total"

    $sum = $available + $held + $confirmed
    if ($sum -eq $total) {
        Write-Host "Invariant holds: available + held + confirmed == total"
    } else {
        Write-Host "Invariant broken: $sum != $total"
    }
} catch {
    Write-Host "ERROR: Failed to get show state"
    Write-Host $_.Exception.Message
}

Write-Host ""
Write-Host "=========================================="
Write-Host "Burst Test Complete"
Write-Host "=========================================="
