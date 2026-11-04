const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
function harness(responses=[]) {
 const values=new Map([['accessToken','legacy'],['refreshToken','legacy-refresh']]);
 const storage={getItem:k=>values.get(k)||null,setItem:(k,v)=>values.set(k,String(v)),removeItem:k=>values.delete(k)};
 const calls=[]; let rejectFetch;
 const context={console,URLSearchParams,Map,Date,Error,Promise,JSON,localStorage:storage,
 window:{location:{port:'8080',pathname:'/login.html',origin:'http://localhost:8080',href:''}},
 navigator:{},fetch:async(url,options)=>{calls.push({url,options});const r=responses.shift();if(r instanceof Error)throw r;return r;},
 document:{},alert(){}};
 vm.createContext(context);
 vm.runInContext(fs.readFileSync('backend/src/main/resources/static/js/api.js','utf8')+'\n'+fs.readFileSync('backend/src/main/resources/static/js/auth.js','utf8')+'\nglobalThis.Api=Api;globalThis.Auth=Auth;',context);
 return {context,calls,values};
}
function response(data,status=200){return{ok:status<400,status,json:async()=>data};}
(async()=>{
 const session={accessToken:'memory-access',refreshToken:'should-not-store',userId:1,email:'patient@example.com',role:'PATIENT'};
 const h=harness();h.context.Auth.setSession(session);
 assert.equal(h.values.has('accessToken'),false,'Access tokens must not persist in localStorage');
 assert.equal(h.values.has('refreshToken'),false,'Refresh tokens must not persist in localStorage');
 assert.equal(h.context.Api._getToken(),'memory-access');
 const x=harness([response({data:{...session,accessToken:'restored'}}),response({data:[]})]);
 await x.context.Api.get('/patients/me');
 assert.equal(x.calls[0].url,'/api/v1/auth/refresh');
 assert.equal(x.calls[0].options.credentials,'same-origin');
 assert.equal(x.calls[0].options.headers['X-CSRF-Token'],'clinic');
 assert.equal(x.calls[1].options.headers.Authorization,'Bearer restored');
 const y=harness([response({message:'Conflict'},409)]);y.context.Auth.setSession(session);
 await assert.rejects(y.context.Api.post('/appointments',{}),e=>e.status===409);
 const z=harness([response({message:'Bad credentials'},401)]);
 await assert.rejects(z.context.Api.auth.login({email:'wrong',password:'bad'}));
 assert.equal(z.calls.length,1,'Anonymous login failure must not refresh or logout');
 const c=harness([response({data:{...session,accessToken:'new'}})]);
 const results=await Promise.all([c.context.Api._tryRefresh(),c.context.Api._tryRefresh()]);
 assert.deepEqual(results,[true,true]);assert.equal(c.calls.length,1,'Concurrent refresh must be single flight');
 console.log('Auth frontend regression: 5 scenarios passed');
})().catch(e=>{console.error(e);process.exitCode=1});
