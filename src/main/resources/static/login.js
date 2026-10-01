document.querySelector('#login-form').addEventListener('submit',async event=>{
 event.preventDefault();const error=document.querySelector('#error');error.textContent='';
 const button=event.target.querySelector('button');button.disabled=true;
 try {const token=await fetch('/api/csrf').then(r=>r.json());const body=new URLSearchParams(new FormData(event.target));body.set(token.parameterName,token.token);
 const response=await fetch('/login',{method:'POST',body});if(response.ok)location.href='/';else error.textContent='Invalid username or password.';
 }catch(e){error.textContent='Unable to connect. Please try again.';}finally{button.disabled=false;}
});