// Creates the existing store layout as editable vector geometry around unchanged app screenshots.
import {createRequire} from 'node:module';
import {readFile,writeFile,mkdir} from 'node:fs/promises';
import {dirname,resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
const here=dirname(fileURLToPath(import.meta.url));
const root=resolve(here,'../../..');
const require=createRequire(resolve(root,'firebase/v2/functions/package.json'));
const sharp=require('sharp');
const xml=await readFile(resolve(root,'composeApp/src/commonMain/composeResources/values/strings.xml'),'utf8');
const strings=new Map([...xml.matchAll(/<string name="([^"]+)">([^<]*)<\/string>/g)].map(m=>[m[1],m[2]]));
const escape=s=>s.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('"','&quot;');
const plans=[{file:'07-social',key:'feed'},{file:'08-social-chat',key:'chat'}];
for(const platform of ['ios','android']){
 const spec=platform==='ios'?{w:1320,h:2868,phoneWidth:1004,y:670,border:16,radius:140,titleSize:96,lines:[225,340],subtitleY:445,subtitleSize:43}:{w:1080,h:1920,phoneWidth:648,y:448,border:12,radius:80,titleSize:68,lines:[155,235],subtitleY:307,subtitleSize:30};
 for(const plan of plans){
  const source=resolve(here,platform,'ham-ekranlar',plan.file+'.png');
  let bytes;try{bytes=await readFile(source);}catch(e){if(e.code==='ENOENT'){console.log('Waiting for capture:',platform,plan.file);continue;}throw e;}
  const image=await sharp(bytes).metadata();
  let innerWidth=spec.phoneWidth-spec.border*2;
  let innerHeight=Math.round(innerWidth*image.height/image.width);
  const maxHeight=spec.h-spec.y-56-spec.border*2;
  if(innerHeight>maxHeight){innerHeight=maxHeight;innerWidth=Math.round(innerHeight*image.width/image.height);}
  const outerWidth=innerWidth+spec.border*2,outerHeight=innerHeight+spec.border*2;
  const x=(spec.w-outerWidth)/2,screenX=x+spec.border,screenY=spec.y+spec.border;
  const radius=Math.min(spec.radius,outerWidth/5);
  const titles=['first','second'].map(part=>strings.get('social_store_'+plan.key+'_title_'+part));
  const subtitle=strings.get('social_store_'+plan.key+'_subtitle');
  if([...titles,subtitle].some(v=>!v))throw new Error('Missing shared social store string');
  const hardware=platform==='ios'?`<rect x="${spec.w/2-130}" y="${screenY+28}" width="260" height="66" rx="33" fill="#000"/>`:`<circle cx="${spec.w/2}" cy="${screenY+20}" r="9" fill="#111"/>`;
  const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="${spec.w}" height="${spec.h}" viewBox="0 0 ${spec.w} ${spec.h}">
<defs><linearGradient id="lime" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stop-color="rgb(201,237,48)"/><stop offset="1" stop-color="rgb(182,224,20)"/></linearGradient><clipPath id="screen"><rect x="${screenX}" y="${screenY}" width="${innerWidth}" height="${innerHeight}" rx="${radius-spec.border}"/></clipPath></defs>
<rect width="100%" height="100%" fill="url(#lime)"/>
<g fill="#006A43" font-family="Arial Rounded MT Bold, Arial, sans-serif" font-weight="bold" text-anchor="middle">${titles.map((text,i)=>`<text x="${spec.w/2}" y="${spec.lines[i]}" font-size="${spec.titleSize}">${escape(text)}</text>`).join('')}<text x="${spec.w/2}" y="${spec.subtitleY}" font-size="${spec.subtitleSize}" font-weight="normal">${escape(subtitle)}</text></g>
<rect x="${x-6}" y="${spec.y+outerHeight*.155}" width="8" height="${outerHeight*.045}" rx="4" fill="#111"/><rect x="${x-6}" y="${spec.y+outerHeight*.21}" width="8" height="${outerHeight*.045}" rx="4" fill="#111"/><rect x="${x+outerWidth-2}" y="${spec.y+outerHeight*.23}" width="8" height="${outerHeight*.09}" rx="4" fill="#111"/>
<rect x="${x}" y="${spec.y}" width="${outerWidth}" height="${outerHeight}" rx="${radius}" fill="#111"/>
<image x="${screenX}" y="${screenY}" width="${innerWidth}" height="${innerHeight}" href="data:image/png;base64,${bytes.toString('base64')}" preserveAspectRatio="xMidYMid meet" clip-path="url(#screen)"/>${hardware}</svg>`;
  await mkdir(resolve(here,platform),{recursive:true});
  await writeFile(resolve(here,platform,plan.file+'.svg'),svg);
  await sharp(Buffer.from(svg)).removeAlpha().png().toFile(resolve(here,platform,plan.file+'.png'));
  const check=await sharp(resolve(here,platform,plan.file+'.png')).metadata();
  if(check.width!==spec.w||check.height!==spec.h||check.hasAlpha||check.channels!==3)throw new Error('Invalid store output');
  console.log('Rendered RGB PNG:',platform,plan.file,check.width,check.height);
 }
}
// A compact overview accompanies each platform's original upload files.
for(const platform of ['ios','android']){
 const posters=[];
 for(const plan of plans){
  try{posters.push(await readFile(resolve(here,platform,plan.file+'.png')));}catch(e){if(e.code!=='ENOENT')throw e;}
 }
 if(posters.length!==plans.length)continue;
 const width=420,height=platform==='ios'?913:747,gap=24;
 const tiles=await Promise.all(posters.map(bytes=>sharp(bytes).resize(width,height,{fit:'contain',background:'#fff'}).png().toBuffer()));
 await sharp({create:{width:width*2+gap,height,channels:3,background:'#fff'}}).composite(tiles.map((input,i)=>({input,left:i*(width+gap),top:0}))).png().toFile(resolve(here,platform+'-onizleme.png'));
}
