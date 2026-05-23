(function () {
  var currentScript = document.currentScript;
  var appId = '${applicationId}';
  var iconPath = '${iconUrl}';
  var srcPrefix = currentScript ? currentScript.src.replace(/\/conversation\/api\/application\/[^/]+\/embed.*$/, '') : window.location.origin;
  var iconUrl = srcPrefix + iconPath.replace(/^\./, '/conversation');
  var baseUrl = srcPrefix;
  function init() {
    var isMobile = function() { return window.innerWidth < 640; };
    var trigger = document.createElement('div');
    trigger.innerHTML = '<img src="' + iconUrl + '" style="width:100%;height:100%;" />';
    var triggerStyle = {
      position:'fixed',bottom:'24px',right:'24px',width:'48px',height:'48px',
      display:'flex',alignItems:'center',
      justifyContent:'center',cursor:'pointer',
      zIndex:'99998',transition:'transform 0.2s'
    };
    Object.assign(trigger.style, triggerStyle);
    trigger.onmouseenter = function(){ trigger.style.transform='scale(1.1)'; };
    trigger.onmouseleave = function(){ trigger.style.transform='scale(1)'; };
    var container = document.createElement('div');
    Object.assign(container.style, {position:'fixed',overflow:'hidden',zIndex:'99999',display:'none',background:'#fff'});
    var applyStyle = function(){
      if(isMobile()){
        container.style.top='0';container.style.left='0';container.style.bottom='auto';
        container.style.right='auto';container.style.width='100%';container.style.height='100%';
        container.style.borderRadius='0';container.style.boxShadow='none';
      }else{
        container.style.top='auto';container.style.left='auto';container.style.bottom='84px';
        container.style.right='24px';container.style.width='400px';container.style.height='600px';
        container.style.borderRadius='12px';container.style.boxShadow='0 8px 32px rgba(0,0,0,0.15)';
      }
    };
    applyStyle();
    var closeBtn = document.createElement('div');
    closeBtn.innerHTML='<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#666" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>';
    Object.assign(closeBtn.style,{position:'absolute',top:'8px',right:'8px',width:'28px',height:'28px',borderRadius:'50%',background:'rgba(255,255,255,0.9)',display:'flex',alignItems:'center',justifyContent:'center',cursor:'pointer',zIndex:'100000',boxShadow:'0 2px 4px rgba(0,0,0,0.1)'});
    var iframe = document.createElement('iframe');
    iframe.src = baseUrl + '/conversation/a/' + appId;
    Object.assign(iframe.style,{width:'100%',height:'100%',border:'none'});
    var isOpen = false;
    trigger.onclick = function(){
      isOpen=!isOpen;
      container.style.display=isOpen?'block':'none';
      if(isOpen) applyStyle();
    };
    closeBtn.onclick = function(e){
      e.stopPropagation();isOpen=false;container.style.display='none';
    };
    window.addEventListener('resize',function(){ if(isOpen) applyStyle(); });
    container.appendChild(closeBtn);
    container.appendChild(iframe);
    document.body.appendChild(trigger);
    document.body.appendChild(container);
  }
  if(document.readyState==='loading'){document.addEventListener('DOMContentLoaded',init);}else{init();}
})();
