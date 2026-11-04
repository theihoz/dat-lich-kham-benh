#!/usr/bin/env python3
"""Read-only prod smoke; optional rate-limit fixture is local QA only."""
import argparse,json,os,time,urllib.request,urllib.error
p=argparse.ArgumentParser();p.add_argument('--base',default='http://localhost:18082');p.add_argument('--output');p.add_argument('--check-local-rate-limit',action='store_true');a=p.parse_args()
secret=os.environ.get('METRICS_TOKEN','')
if len(secret)<32:raise SystemExit('Set METRICS_TOKEN from a secure environment')
base=a.base.rstrip('/')
def request(path,token=None,body=None):
 headers={'Authorization':'Bearer '+token} if token else {}
 if body is not None:headers['Content-Type']='application/json'
 r=urllib.request.Request(base+path,headers=headers,data=json.dumps(body).encode() if body is not None else None)
 try:
  with urllib.request.urlopen(r,timeout=10) as response:return response.status,dict(response.headers),response.read()
 except urllib.error.HTTPError as error:return error.code,dict(error.headers),error.read()
for attempt in range(20):
 try:
  if request('/actuator/health/readiness')[0]==200:break
 except OSError:pass
 time.sleep(1)
assert request('/actuator/prometheus')[0]==401
assert request('/actuator/prometheus',secret)[0]==200
assert request('/api/v1/admin/stats',secret)[0]==401
assert request('/patterns.html')[0]==404
status,headers,_=request('/login.html');assert status==200
assert "script-src 'self'" in headers.get('Content-Security-Policy','')
assert 'unsafe-inline' not in headers['Content-Security-Policy'].split('script-src',1)[1].split(';',1)[0]
assert request('/api/v1/clinic-info')[1].get('Cache-Control')=='no-store'
result={'metricsUnauthorized':401,'metricsScopedAuthorized':200,'metricsCannotAccessAdmin':401,'demoPageBlocked':404,'strictScriptCsp':True,'apiNoStore':True,'profile':'prod'}
if a.check_local_rate_limit:
 if base not in ['http://localhost:18082','http://127.0.0.1:18082']:raise SystemExit('Rate-limit fixture is restricted to the isolated local QA port')
 remaining=60-time.time()%60
 if remaining<4:time.sleep(remaining+0.1)
 statuses=[request('/api/v1/auth/login',body={'email':'rate-limit-probe@example.test','password':'test-invalid-password'})[0] for _ in range(11)]
 assert statuses[-1]==429, statuses
 result['realRedisRateLimit']=429
if a.output:
 with open(a.output,'w') as f:json.dump(result,f,indent=2);f.write('\n')
print(json.dumps(result))
