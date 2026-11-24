/* Relatório+ — exportação local, sem enviar o documento a terceiros. */
(function(){
'use strict';
const enc=new TextEncoder(), bytes=s=>enc.encode(s), xml=s=>String(s??'').replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g,'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&apos;'}[c]));
function download(blob,name){const u=URL.createObjectURL(blob),a=document.createElement('a');a.href=u;a.download=name;document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(u),60000);}
function filename(d){return ['Relatório',d.oficina,d.mes,d.ano].filter(Boolean).join(' - ').replace(/[<>:"/\\|?*\u0000-\u001f]/g,'_').slice(0,160);}
function dataBytes(url){return Uint8Array.from(atob(url.split(',')[1]),c=>c.charCodeAt(0));}
async function paintPage(page){
 const base=page.getBoundingClientRect();if(!base.width||!base.height)throw Error('Prévia não está disponível.');
 const canvas=document.createElement('canvas'),scale=3;canvas.width=Math.round(base.width*scale);canvas.height=Math.round(base.height*scale);const ctx=canvas.getContext('2d',{alpha:false});ctx.scale(scale,scale);ctx.fillStyle='#ffffff';ctx.fillRect(0,0,base.width,base.height);
 const xy=r=>({x:r.left-base.left,y:r.top-base.top,w:r.width,h:r.height});
 for(const im of page.querySelectorAll('img')){if(!im.complete)await im.decode();if(!im.naturalWidth)throw Error('Imagem institucional indisponível.');const r=xy(im.getBoundingClientRect());ctx.save();if(im.classList.contains('header-strip')){ctx.beginPath();ctx.rect(r.x,r.y,r.w,r.h*.9);ctx.clip();}ctx.drawImage(im,r.x,r.y,r.w,r.h);ctx.restore();}
 for(const el of page.querySelectorAll('.frame,.row,.foot')){
  const cs=getComputedStyle(el);if(cs.visibility==='hidden')continue;const r=xy(el.getBoundingClientRect());
  for(const [side,x1,y1,x2,y2] of [['Top',r.x,r.y,r.x+r.w,r.y],['Right',r.x+r.w,r.y,r.x+r.w,r.y+r.h],['Bottom',r.x,r.y+r.h,r.x+r.w,r.y+r.h],['Left',r.x,r.y,r.x,r.y+r.h]]){const w=parseFloat(cs['border'+side+'Width']);if(!w||cs['border'+side+'Style']==='none')continue;ctx.lineWidth=w;ctx.strokeStyle=cs['border'+side+'Color'];ctx.beginPath();ctx.moveTo(x1,y1);ctx.lineTo(x2,y2);ctx.stroke();}
 }
 const walker=document.createTreeWalker(page,NodeFilter.SHOW_TEXT);let n;
 while(n=walker.nextNode()){
  const p=n.parentElement,cs=getComputedStyle(p);if(cs.visibility==='hidden'||cs.display==='none'||!n.nodeValue.trim())continue;
  ctx.font=[cs.fontStyle,cs.fontWeight,cs.fontSize,cs.fontFamily].join(' ');ctx.fillStyle=cs.color;ctx.textBaseline='alphabetic';
  let line=null,range=document.createRange();const txt=n.nodeValue;
  function flush(){if(!line||!line.text.trim())return;const m=ctx.measureText(line.text),a=m.fontBoundingBoxAscent??parseFloat(cs.fontSize)*.8,b=m.fontBoundingBoxDescent??parseFloat(cs.fontSize)*.2;const baseline=line.y+(line.h-a-b)/2+a;ctx.fillText(line.text,line.x,baseline,Math.max(line.right-line.x,0.1));}
  for(let i=0;i<txt.length;){const cp=txt.codePointAt(i),len=cp>65535?2:1,ch=txt.slice(i,i+len);range.setStart(n,i);range.setEnd(n,i+len);i+=len;const rr=range.getBoundingClientRect();if(ch==='\n'||ch==='\r'){flush();line=null;continue;}if(!rr.width||!rr.height)continue;const r=xy(rr);if(!line||Math.abs(line.y-r.y)>2){flush();line={text:ch,x:r.x,y:r.y,h:r.h,right:r.x+r.w};}else{line.text+=ch;line.right=Math.max(line.right,r.x+r.w);}}
  flush();range.detach();
 }
 const blob=await new Promise((resolve,reject)=>canvas.toBlob(b=>b?resolve(b):reject(Error('Falha ao renderizar página.')),'image/jpeg',.96));return {width:canvas.width,height:canvas.height,data:new Uint8Array(await blob.arrayBuffer())};
}
function pdfBlob(images){
 const parts=[],offsets=[0];let offset=0;const add=v=>{const a=typeof v==='string'?bytes(v):v;parts.push(a);offset+=a.length;};
 add('%PDF-1.4\n');add(new Uint8Array([37,226,227,207,211,10]));
 function obj(id,content,stream){offsets[id]=offset;add(id+' 0 obj\n');add(content);if(stream){add('\nstream\n');add(stream);add('\nendstream');}add('\nendobj\n');}
 obj(1,'<< /Type /Catalog /Pages 2 0 R >>');obj(2,'<< /Type /Pages /Count '+images.length+' /Kids ['+images.map((_,i)=>(3+i*3)+' 0 R').join(' ')+'] >>');
 images.forEach((im,i)=>{const p=3+i*3,c=p+1,j=p+2;obj(p,'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595.276 841.89] /Resources << /XObject << /Im0 '+j+' 0 R >> >> /Contents '+c+' 0 R >>');const cmd=bytes('q\n595.276 0 0 841.89 0 0 cm\n/Im0 Do\nQ\n');obj(c,'<< /Length '+cmd.length+' >>',cmd);obj(j,'<< /Type /XObject /Subtype /Image /Width '+im.width+' /Height '+im.height+' /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length '+im.data.length+' >>',im.data);});
 const xref=offset;add('xref\n0 '+offsets.length+'\n0000000000 65535 f \n');for(let i=1;i<offsets.length;i++)add(String(offsets[i]).padStart(10,'0')+' 00000 n \n');add('trailer\n<< /Size '+offsets.length+' /Root 1 0 R >>\nstartxref\n'+xref+'\n%%EOF\n');return new Blob(parts,{type:'application/pdf'});
}
const crcTable=Uint32Array.from({length:256},(_,i)=>{let c=i;for(let k=0;k<8;k++)c=(c&1)?0xedb88320^(c>>>1):c>>>1;return c>>>0;});
function crc(a){let c=0xffffffff;for(const v of a)c=crcTable[(c^v)&255]^(c>>>8);return (c^0xffffffff)>>>0;}
function zip(files){
 let pos=0,centralSize=0;const parts=[],central=[];
 for(const [name,content] of Object.entries(files)){const n=bytes(name),a=typeof content==='string'?bytes(content):content,check=crc(a),h=new Uint8Array(30+n.length),v=new DataView(h.buffer);v.setUint32(0,0x04034b50,true);v.setUint16(4,20,true);v.setUint16(6,0x800,true);v.setUint16(12,0x21,true);v.setUint32(14,check,true);v.setUint32(18,a.length,true);v.setUint32(22,a.length,true);v.setUint16(26,n.length,true);h.set(n,30);parts.push(h,a);
 const c=new Uint8Array(46+n.length),w=new DataView(c.buffer);w.setUint32(0,0x02014b50,true);w.setUint16(4,20,true);w.setUint16(6,20,true);w.setUint16(8,0x800,true);w.setUint16(14,0x21,true);w.setUint32(16,check,true);w.setUint32(20,a.length,true);w.setUint32(24,a.length,true);w.setUint16(28,n.length,true);w.setUint32(42,pos,true);c.set(n,46);central.push(c);centralSize+=c.length;pos+=h.length+a.length;}
 const e=new Uint8Array(22),v=new DataView(e.buffer);v.setUint32(0,0x06054b50,true);v.setUint16(8,central.length,true);v.setUint16(10,central.length,true);v.setUint32(12,centralSize,true);v.setUint32(16,pos,true);return new Blob([...parts,...central,e],{type:'application/vnd.openxmlformats-officedocument.wordprocessingml.document'});
}
const X='<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',W='http://schemas.openxmlformats.org/wordprocessingml/2006/main',R='http://schemas.openxmlformats.org/officeDocument/2006/relationships',P='http://schemas.openxmlformats.org/package/2006/relationships',A='http://schemas.openxmlformats.org/drawingml/2006/main',WP='http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing',PIC='http://schemas.openxmlformats.org/drawingml/2006/picture';
function run(s,opt={}){return '<w:r><w:rPr><w:rFonts w:ascii="Arial" w:hAnsi="Arial"/>'+(opt.bold?'<w:b/>':'')+(opt.italic?'<w:i/>':'')+'<w:sz w:val="'+(opt.size||24)+'"/><w:color w:val="'+(opt.color||'000000')+'"/></w:rPr>'+String(s??'').split('\n').map((x,i)=>(i?'<w:br/>':'')+'<w:t xml:space="preserve">'+xml(x)+'</w:t>').join('')+'</w:r>';}
function para(s,opt={}){return '<w:p><w:pPr><w:spacing w:before="'+(opt.before||0)+'" w:after="'+(opt.after??100)+'" w:line="276" w:lineRule="auto"/>'+(opt.center?'<w:jc w:val="center"/>':'')+(opt.keep?'<w:keepNext/>':'')+(opt.indent?'<w:ind w:left="240" w:hanging="180"/>':'')+'</w:pPr>'+run(s,opt)+'</w:p>';}
function picture(id,cx,cy,anchor=false){const graphic='<a:graphic><a:graphicData uri="'+PIC+'"><pic:pic><pic:nvPicPr><pic:cNvPr id="0" name="'+id+'"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip r:embed="'+id+'"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="'+cx+'" cy="'+cy+'"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic>';
 const extent='<wp:extent cx="'+cx+'" cy="'+cy+'"/><wp:effectExtent l="0" t="0" r="0" b="0"/>';
 return '<w:r><w:drawing>'+(anchor?'<wp:anchor distT="0" distB="0" distL="0" distR="0" simplePos="0" relativeHeight="0" behindDoc="1" locked="0" layoutInCell="1" allowOverlap="1"><wp:simplePos x="0" y="0"/><wp:positionH relativeFrom="page"><wp:align>center</wp:align></wp:positionH><wp:positionV relativeFrom="page"><wp:align>center</wp:align></wp:positionV>'+extent+'<wp:wrapNone/><wp:docPr id="2" name="Marca d’água"/><wp:cNvGraphicFramePr/>'+graphic+'</wp:anchor>':'<wp:inline distT="0" distB="0" distL="0" distR="0">'+extent+'<wp:docPr id="1" name="Cabeçalho institucional"/><wp:cNvGraphicFramePr/>'+graphic+'</wp:inline>')+'</w:drawing></w:r>';
}
function docxBlob(d,header,watermark,materials,style,dimensions){
 const ns=' xmlns:w="'+W+'" xmlns:r="'+R+'" xmlns:wp="'+WP+'" xmlns:a="'+A+'" xmlns:pic="'+PIC+'"';
 const relationship=(id,type,target)=>'<Relationship Id="'+id+'" Type="'+R+'/'+type+'" Target="'+target+'"/>';
 const rels=s=>X+'<Relationships xmlns="'+P+'">'+s+'</Relationships>';
 const headerCx=6840000,headerCy=Math.round(headerCx*dimensions.headerH/dimensions.headerW),waterCx=5580000,waterCy=Math.round(waterCx*dimensions.waterH/dimensions.waterW);
 const headerXml=X+'<w:hdr'+ns+'><w:p><w:pPr><w:spacing w:after="0"/><w:jc w:val="center"/></w:pPr>'+picture('rId1',headerCx,headerCy)+picture('rId2',waterCx,waterCy,true)+'</w:p></w:hdr>';
 const footerLines=['“O amor nasce com a doação.”','Associação de Proteção à Infância e de Assistência Social de Santa Luzia','R. Floriano Peixoto 409 – Centro - Santa Luzia – MG - CEP 33.010-030 –','Tel. /Fax 3641-1078/98993-0383','CNPJ 24 427 155/0001-77 - e mail: pfinanceiro914@gmail.com'];
 const footerXml=X+'<w:ftr'+ns+'>'+footerLines.map((s,i)=>para(s,{center:true,size:20,color:'17365D',italic:i===0,after:0})).join('')+'<w:p><w:pPr><w:jc w:val="right"/><w:spacing w:after="0"/></w:pPr>'+run('Página ',{size:18})+'<w:fldSimple w:instr="PAGE"><w:r><w:t>1</w:t></w:r></w:fldSimple>'+run(' de ',{size:18})+'<w:fldSimple w:instr="NUMPAGES"><w:r><w:t>1</w:t></w:r></w:fldSimple></w:p></w:ftr>';
 function row(content){return '<w:tr><w:tc><w:tcPr><w:tcW w:w="10400" w:type="dxa"/><w:tcMar><w:top w:w="80" w:type="dxa"/><w:left w:w="100" w:type="dxa"/><w:bottom w:w="80" w:type="dxa"/><w:right w:w="100" w:type="dxa"/></w:tcMar></w:tcPr>'+content+'</w:tc></w:tr>';}
 const borders=['top','left','bottom','right','insideH','insideV'].map(s=>'<w:'+s+' w:val="single" w:sz="4" w:color="000000"/>').join('');
 let content=row(para('SERVIÇO DE CONVIVÊNCIA E FORTALECIMENTO DE VÍNCULOS\n'+d.ano,{center:true,bold:true,after:0}))+row(para('OFICINA: '+d.oficina,{after:0}))+row(para('OBJETIVO: '+d.objetivo,{after:0}))+row(para('RESPONSÁVEL: '+d.responsavel,{after:0}))+row(para('MÊS: '+String(d.mes).toUpperCase()+'                 ANO: '+d.ano,{after:0}));
 let body='';for(const a of d.activities||[]){if(!a.date&&!a.what&&!a.how)continue;body+=para('OFICINA'+(a.date?' '+a.date.split('-').reverse().join('/'):'')+':',{bold:true,keep:true,before:100});if(a.what)body+=para((style==='topics'?'• ':'')+a.what);if(a.how)body+=para((style==='topics'?'• ':'')+a.how);}
 if(materials.length){body+=para('Materiais Utilizados',{bold:true,keep:true,before:160,after:80});body+=materials.map((m,i)=>para('• '+m+(i===materials.length-1?'.':';'),{indent:true,after:20})).join('');}content+=row(body||para(''));content+=row(para('Assinatura: ',{before:120,after:300}));
 const doc=X+'<w:document'+ns+'><w:body>'+para('RELATÓRIO MENSAL DE ATIVIDADES',{center:true,bold:true,size:28,keep:true})+'<w:tbl><w:tblPr><w:tblW w:w="10400" w:type="dxa"/><w:tblBorders>'+borders+'</w:tblBorders><w:tblLayout w:type="fixed"/></w:tblPr><w:tblGrid><w:gridCol w:w="10400"/></w:tblGrid>'+content+'</w:tbl><w:p/><w:sectPr><w:headerReference w:type="default" r:id="rId1"/><w:footerReference w:type="default" r:id="rId2"/><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="2100" w:right="753" w:bottom="1800" w:left="753" w:header="300" w:footer="300" w:gutter="0"/></w:sectPr></w:body></w:document>';
 const ct=X+'<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Default Extension="png" ContentType="image/png"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/header1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"/><Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/></Types>';
 return zip({'[Content_Types].xml':ct,'_rels/.rels':rels(relationship('rId1','officeDocument','word/document.xml')),'word/document.xml':doc,'word/_rels/document.xml.rels':rels(relationship('rId1','header','header1.xml')+relationship('rId2','footer','footer1.xml')),'word/header1.xml':headerXml,'word/_rels/header1.xml.rels':rels(relationship('rId1','image','media/header.png')+relationship('rId2','image','media/watermark.png')),'word/footer1.xml':footerXml,'word/media/header.png':dataBytes(header),'word/media/watermark.png':dataBytes(watermark)});
}
async function imageDimensions(src){const im=new Image();im.src=src;await im.decode();return [im.naturalWidth,im.naturalHeight];}
window.RelatorioExport={
 async pdf(pages,d,onProgress){const images=[];for(let i=0;i<pages.length;i++){onProgress(i,pages.length);images.push(await paintPage(pages[i]));}onProgress(pages.length,pages.length);download(pdfBlob(images),filename(d)+'.pdf');},
 async word(d,header,watermark,materials,style){const [headerW,headerH]=await imageDimensions(header),[waterW,waterH]=await imageDimensions(watermark);download(docxBlob(d,header,watermark,materials,style,{headerW,headerH,waterW,waterH}),filename(d)+'.docx');}
};
})();
