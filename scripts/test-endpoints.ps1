$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8090/api/v1'
$results = New-Object System.Collections.Generic.List[object]

function Add-Result($name,$expected,$actual,$ok,$detail){
  $results.Add([pscustomobject]@{Test=$name;Expected=$expected;Actual=$actual;Pass=$ok;Detail=$detail}) | Out-Null
}

function Invoke-CurlJson {
  param(
    [string]$Name,
    [string]$Method,
    [string]$Url,
    [string]$Token,
    [string]$Body,
    [int[]]$ExpectedCodes
  )
  $tmp = [System.IO.Path]::GetTempFileName()
  $args = @('-s','--max-time','25','-o',$tmp,'-w','%{http_code}','-X',$Method,$Url,'-H','Content-Type: application/json')
  if($Token){ $args += @('-H',"Authorization: Bearer $Token") }
  $bodyFile = $null
  if($Body){
    $bodyFile = [System.IO.Path]::GetTempFileName()
    Set-Content -Path $bodyFile -Value $Body -NoNewline
    $args += @('--data-binary',"@$bodyFile")
  }
  $code = (& curl.exe @args)
  $content = Get-Content $tmp -Raw
  Remove-Item $tmp -Force
  if($bodyFile -and (Test-Path $bodyFile)){ Remove-Item $bodyFile -Force }
  $ok = $ExpectedCodes -contains [int]$code
  $detail = if($content.Length -gt 250){ $content.Substring(0,250) } else { $content }
  Add-Result $Name ($ExpectedCodes -join '/') $code $ok $detail
  return [pscustomobject]@{Code=[int]$code;Body=$content}
}

function Invoke-CurlForm {
  param(
    [string]$Name,
    [string]$Url,
    [string]$Token,
    [hashtable]$Fields,
    [string]$FilePath,
    [int[]]$ExpectedCodes
  )
  $tmp = [System.IO.Path]::GetTempFileName()
  $args = @('-s','--max-time','25','-o',$tmp,'-w','%{http_code}','-X','POST',$Url)
  if($Token){ $args += @('-H',"Authorization: Bearer $Token") }
  foreach($k in $Fields.Keys){ $args += @('-F',"$k=$($Fields[$k])") }
  if($FilePath){ $args += @('-F',"resumeFile=@$FilePath;type=text/plain") }
  $code = (& curl.exe @args)
  $content = Get-Content $tmp -Raw
  Remove-Item $tmp -Force
  $ok = $ExpectedCodes -contains [int]$code
  $detail = if($content.Length -gt 250){ $content.Substring(0,250) } else { $content }
  Add-Result $Name ($ExpectedCodes -join '/') $code $ok $detail
  return [pscustomobject]@{Code=[int]$code;Body=$content}
}

$stamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$recruiterEmail = "recruiter.$stamp@talentforge.dev"
$candidateEmail = "candidate.$stamp@talentforge.dev"
$applicantEmail = "applicant.$stamp@talentforge.dev"

# Auth login admin
$body = @{email='admin@talentforge.local'; password='Admin@123'} | ConvertTo-Json -Compress
$r = Invoke-CurlJson -Name 'auth.login.admin' -Method 'POST' -Url "$base/auth/login" -Body $body -ExpectedCodes @(200)
$adminToken = ''
if($r.Code -eq 200){ $adminToken = ((($r.Body | ConvertFrom-Json).data).token) }

# Register candidate
$body = @{email=$candidateEmail;password='Pass@123';fullName='Candidate Test';role='CANDIDATE'} | ConvertTo-Json -Compress
Invoke-CurlJson -Name 'auth.register.candidate' -Method 'POST' -Url "$base/auth/register" -Body $body -ExpectedCodes @(200) | Out-Null

# Create recruiter
$body = @{email=$recruiterEmail;password='Pass@123';fullName='Recruiter Test';role='RECRUITER';company='TalentForge'} | ConvertTo-Json -Compress
$r = Invoke-CurlJson -Name 'users.create.recruiter' -Method 'POST' -Url "$base/users" -Token $adminToken -Body $body -ExpectedCodes @(200)
$recruiterId = $null
if($r.Code -eq 200){ $recruiterId = ((($r.Body | ConvertFrom-Json).data).id) }

# Login recruiter
$body = @{email=$recruiterEmail;password='Pass@123'} | ConvertTo-Json -Compress
$r = Invoke-CurlJson -Name 'auth.login.recruiter' -Method 'POST' -Url "$base/auth/login" -Body $body -ExpectedCodes @(200)
$recruiterToken = ''
if($r.Code -eq 200){ $recruiterToken = ((($r.Body | ConvertFrom-Json).data).token) }

# Forbidden and user reads
$body = @{email='x@x.com';password='x';fullName='x';role='CANDIDATE'} | ConvertTo-Json -Compress
Invoke-CurlJson -Name 'users.create.forbidden.for.recruiter' -Method 'POST' -Url "$base/users" -Token $recruiterToken -Body $body -ExpectedCodes @(403) | Out-Null
Invoke-CurlJson -Name 'users.get.all' -Method 'GET' -Url "$base/users" -Token $adminToken -ExpectedCodes @(200) | Out-Null
if($recruiterId){ Invoke-CurlJson -Name 'users.get.by.id' -Method 'GET' -Url "$base/users/$recruiterId" -Token $adminToken -ExpectedCodes @(200) | Out-Null }

$body = @{email="manual.$stamp@talentforge.dev";password='Pass@123';fullName='Manual User';role='CANDIDATE'} | ConvertTo-Json -Compress
$r = Invoke-CurlJson -Name 'users.create.candidate' -Method 'POST' -Url "$base/users" -Token $adminToken -Body $body -ExpectedCodes @(200)
$manualUserId = $null
if($r.Code -eq 200){ $manualUserId = ((($r.Body | ConvertFrom-Json).data).id) }
if($manualUserId){ Invoke-CurlJson -Name 'users.deactivate' -Method 'POST' -Url "$base/users/$manualUserId/deactivate" -Token $adminToken -ExpectedCodes @(200) | Out-Null }

# Jobs
$body = @{title='Java Backend Engineer';description='Build APIs and services';requirements='Spring Boot, PostgreSQL';location='Lagos';department='Engineering';salaryRange='5M-8M NGN';jobType='FULL_TIME';experienceLevel='MID';status='OPEN';recruiterId=$recruiterId} | ConvertTo-Json -Compress
$r = Invoke-CurlJson -Name 'jobs.create' -Method 'POST' -Url "$base/jobs" -Token $recruiterToken -Body $body -ExpectedCodes @(200)
$jobId = $null
if($r.Code -eq 200){ $jobId = ((($r.Body | ConvertFrom-Json).data).id) }
if($jobId){
  $body = @{title='Senior Java Backend Engineer';description='Build scalable APIs';requirements='Spring Boot, PostgreSQL, Docker';location='Remote';department='Engineering';salaryRange='8M-12M NGN';jobType='FULL_TIME';experienceLevel='SENIOR';status='OPEN';recruiterId=$recruiterId} | ConvertTo-Json -Compress
  Invoke-CurlJson -Name 'jobs.update' -Method 'PUT' -Url "$base/jobs/$jobId" -Token $recruiterToken -Body $body -ExpectedCodes @(200) | Out-Null
  Invoke-CurlJson -Name 'jobs.get.all' -Method 'GET' -Url "$base/jobs" -Token $recruiterToken -ExpectedCodes @(200) | Out-Null
  Invoke-CurlJson -Name 'jobs.get.by.recruiter' -Method 'GET' -Url "$base/jobs?recruiterId=$recruiterId" -Token $recruiterToken -ExpectedCodes @(200) | Out-Null
  Invoke-CurlJson -Name 'jobs.get.by.id' -Method 'GET' -Url "$base/jobs/$jobId" -Token $recruiterToken -ExpectedCodes @(200) | Out-Null
}

# Applicants
$body = @{fullName='Applicant One';email=$applicantEmail;phone='08012345678';location='Abuja';skills='Java,Spring';yearsOfExperience=4} | ConvertTo-Json -Compress
$r = Invoke-CurlJson -Name 'applicants.create' -Method 'POST' -Url "$base/applicants" -Token $recruiterToken -Body $body -ExpectedCodes @(200)
$applicantId = $null
if($r.Code -eq 200){ $applicantId = ((($r.Body | ConvertFrom-Json).data).id) }
if($applicantId){
  $body = @{fullName='Applicant One Updated';email=$applicantEmail;phone='08099999999';location='Lagos';skills='Java,Spring,SQL';yearsOfExperience=5} | ConvertTo-Json -Compress
  Invoke-CurlJson -Name 'applicants.update' -Method 'PUT' -Url "$base/applicants/$applicantId" -Token $recruiterToken -Body $body -ExpectedCodes @(200) | Out-Null
  Invoke-CurlJson -Name 'applicants.get.all' -Method 'GET' -Url "$base/applicants" -Token $recruiterToken -ExpectedCodes @(200) | Out-Null
  Invoke-CurlJson -Name 'applicants.get.by.id' -Method 'GET' -Url "$base/applicants/$applicantId" -Token $recruiterToken -ExpectedCodes @(200) | Out-Null
}

# Applications multipart
$resumePath = Join-Path (Get-Location) "resume-$stamp.txt"
"Experienced Java engineer with Spring Boot and PostgreSQL." | Set-Content -Path $resumePath
$applicationId = $null
if($jobId -and $applicantId){
  $fields = @{jobId=$jobId;applicantId=$applicantId;status='APPLIED';coverLetter='I am interested in this role.'}
  $r = Invoke-CurlForm -Name 'applications.submit.multipart' -Url "$base/applications" -Token $recruiterToken -Fields $fields -FilePath $resumePath -ExpectedCodes @(200)
  if($r.Code -eq 200){ $applicationId = ((($r.Body | ConvertFrom-Json).data).id) }
}

if($applicationId){
  $body = @{status='REVIEWING'} | ConvertTo-Json -Compress
  Invoke-CurlJson -Name 'applications.update.status' -Method 'PATCH' -Url "$base/applications/$applicationId/status" -Token $recruiterToken -Body $body -ExpectedCodes @(200) | Out-Null
  Invoke-CurlJson -Name 'applications.get.by.id' -Method 'GET' -Url "$base/applications/$applicationId" -Token $recruiterToken -ExpectedCodes @(200) | Out-Null
  Invoke-CurlJson -Name 'applications.get.by.job' -Method 'GET' -Url "$base/applications?jobId=$jobId" -Token $recruiterToken -ExpectedCodes @(200) | Out-Null
  Invoke-CurlJson -Name 'applications.get.by.applicant' -Method 'GET' -Url "$base/applications?applicantId=$applicantId" -Token $recruiterToken -ExpectedCodes @(200) | Out-Null

  $body = @{applicationId=$applicationId;recruiterId=$recruiterId;content='Strong resume, proceed to screening.'} | ConvertTo-Json -Compress
  Invoke-CurlJson -Name 'notes.create' -Method 'POST' -Url "$base/notes" -Token $recruiterToken -Body $body -ExpectedCodes @(200) | Out-Null
  Invoke-CurlJson -Name 'notes.get.by.application' -Method 'GET' -Url "$base/notes/application/$applicationId" -Token $recruiterToken -ExpectedCodes @(200) | Out-Null

  $body = @{applicationId=$applicationId;scheduledAt='2026-03-01T10:00:00';interviewType='VIDEO';meetingLink='https://meet.example.com/abc';status='SCHEDULED';feedback=''} | ConvertTo-Json -Compress
  Invoke-CurlJson -Name 'interviews.create' -Method 'POST' -Url "$base/interviews" -Token $recruiterToken -Body $body -ExpectedCodes @(200) | Out-Null
  Invoke-CurlJson -Name 'interviews.get.by.application' -Method 'GET' -Url "$base/interviews/application/$applicationId" -Token $recruiterToken -ExpectedCodes @(200) | Out-Null
}

if($recruiterId){
  $body = @{userId=$recruiterId;planType='PRO';jobPostLimit=100;applicantLimit=500;paymentReference="PAY-$stamp";active=$true} | ConvertTo-Json -Compress
  Invoke-CurlJson -Name 'subscriptions.upsert' -Method 'POST' -Url "$base/subscriptions" -Token $recruiterToken -Body $body -ExpectedCodes @(200) | Out-Null
  Invoke-CurlJson -Name 'subscriptions.get.by.user' -Method 'GET' -Url "$base/subscriptions/user/$recruiterId" -Token $recruiterToken -ExpectedCodes @(200) | Out-Null

  $body = @{userId=$recruiterId;message='How do I shortlist candidates quickly?'} | ConvertTo-Json -Compress
  Invoke-CurlJson -Name 'chat.ask' -Method 'POST' -Url "$base/chat" -Token $recruiterToken -Body $body -ExpectedCodes @(200) | Out-Null
  Invoke-CurlJson -Name 'chat.history' -Method 'GET' -Url "$base/chat/$recruiterId" -Token $recruiterToken -ExpectedCodes @(200) | Out-Null
}

$body = @{title='x'} | ConvertTo-Json -Compress
Invoke-CurlJson -Name 'jobs.create.unauthenticated' -Method 'POST' -Url "$base/jobs" -Body $body -ExpectedCodes @(401,403) | Out-Null

if($jobId){ Invoke-CurlJson -Name 'jobs.delete' -Method 'DELETE' -Url "$base/jobs/$jobId" -Token $recruiterToken -ExpectedCodes @(200) | Out-Null }
if($applicantId){ Invoke-CurlJson -Name 'applicants.delete' -Method 'DELETE' -Url "$base/applicants/$applicantId" -Token $recruiterToken -ExpectedCodes @(200) | Out-Null }

if(Test-Path $resumePath){ Remove-Item $resumePath -Force }

$passed = ($results | Where-Object {$_.Pass}).Count
$total = $results.Count
"TOTAL=$total PASSED=$passed FAILED=$($total-$passed)"
$results | Format-Table -AutoSize
if($total -ne $passed){
  "FAILED_DETAILS"
  $results | Where-Object {-not $_.Pass} | Format-List
}
