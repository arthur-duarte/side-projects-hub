import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:audioplayers/audioplayers.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:image_picker/image_picker.dart';
import 'package:share_plus/share_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';

const violet = Color(0xFF7357E8);
const ink = Color(0xFF1A1A2E);
const languages = <String, String>{'pt-BR': '🇧🇷 Português', 'en': '🇺🇸 English', 'es': '🇪🇸 Español'};
const copy = <String, Map<String, String>>{
  'pt-BR': {
    'brand':'FiguFábrica','tag':'Criador de figurinhas','create':'Criar','packs':'Pacotes','sounds':'Sons','settings':'Idioma','headline':'Sua próxima figurinha começa aqui.','intro':'Escolha uma foto, escreva uma frase e crie algo para mandar no grupo.','choosePhoto':'Escolher foto','caption':'Texto da figurinha','captionHint':'Ex.: PARTIU!','packName':'Nome do pacote','packHint':'Meu pacote','preview':'Prévia','transparent':'Fundo transparente','save':'Salvar no pacote','share':'Compartilhar PNG','clear':'Limpar','saved':'Figurinha salva no pacote!','needImage':'Escolha uma foto ou escreva uma frase.','chooseLanguage':'Escolha o idioma','emptyPacks':'Seus pacotes aparecem aqui.','newPack':'Novo pacote','noStickers':'Nenhuma figurinha neste pacote ainda.','soundIntro':'Efeitos originais, criados no próprio app.','soundLimit':'Estes sons são prévias. O formato público de pacotes de terceiros não garante áudio embutido; use o compartilhamento como PNG ou vídeo quando o destino não aceitar som.','privacy':'As imagens ficam no seu aparelho.','image':'Imagem','textColor':'Cor do texto','white':'Branco','yellow':'Amarelo','coral':'Coral','dark':'Escuro','sound1':'Plim','sound2':'Boing','sound3':'Erro digital','sound4':'Suspense','sound5':'Tambor rápido','sound6':'Aplausos','sound7':'Grilos','sound8':'Fanfarrinha','savedImage':'Arquivo pronto para compartilhar.','delete':'Excluir pacote','removePhoto':'Remover foto','addFirst':'Crie uma figurinha para começar.','packCount':'figurinhas','packExists':'Pacote atualizado.'},
  'en': {
    'brand':'FiguFábrica','tag':'Sticker maker','create':'Create','packs':'My packs','sounds':'Sounds','settings':'Language','headline':'Your next sticker starts here.','intro':'Choose a photo, add a phrase and make something for the group chat.','choosePhoto':'Choose a photo','caption':'Sticker text','captionHint':'e.g. LET’S GO!','packName':'Pack name','packHint':'My pack','preview':'Preview','transparent':'Transparent background','save':'Save to pack','share':'Share PNG','clear':'Clear','saved':'Sticker saved to pack!','needImage':'Choose a photo or write a phrase.','chooseLanguage':'Choose your language','emptyPacks':'Your packs will appear here.','newPack':'New pack','noStickers':'No stickers in this pack yet.','soundIntro':'Original effects made in the app.','soundLimit':'These sounds are previews. The public third-party pack format does not guarantee embedded audio; share as PNG or video if the destination does not support sound.','privacy':'Your images stay on your device.','image':'Image','textColor':'Text color','white':'White','yellow':'Yellow','coral':'Coral','dark':'Dark','sound1':'Ting','sound2':'Boing','sound3':'Digital error','sound4':'Suspense','sound5':'Quick drumroll','sound6':'Applause','sound7':'Crickets','sound8':'Mini fanfare','savedImage':'File ready to share.','delete':'Delete pack','removePhoto':'Remove photo','addFirst':'Create a sticker to get started.','packCount':'stickers','packExists':'Pack updated.'},
  'es': {
    'brand':'FiguFábrica','tag':'Creador de stickers','create':'Crear','packs':'Mis paquetes','sounds':'Sonidos','settings':'Idioma','headline':'Tu próximo sticker empieza aquí.','intro':'Elige una foto, escribe una frase y crea algo para enviar al grupo.','choosePhoto':'Elegir foto','caption':'Texto del sticker','captionHint':'Ej.: ¡VAMOS!','packName':'Nombre del paquete','packHint':'Mi paquete','preview':'Vista previa','transparent':'Fondo transparente','save':'Guardar en paquete','share':'Compartir PNG','clear':'Limpiar','saved':'¡Sticker guardado en el paquete!','needImage':'Elige una foto o escribe una frase.','chooseLanguage':'Elige tu idioma','emptyPacks':'Tus paquetes aparecerán aquí.','newPack':'Nuevo paquete','noStickers':'Todavía no hay stickers en este paquete.','soundIntro':'Efectos originales, creados en la app.','soundLimit':'Estos sonidos son vistas previas. El formato público de paquetes de terceros no garantiza audio integrado; comparte como PNG o video si el destino no admite sonido.','privacy':'Tus imágenes se quedan en tu dispositivo.','image':'Imagen','textColor':'Color del texto','white':'Blanco','yellow':'Amarillo','coral':'Coral','dark':'Oscuro','sound1':'Plim','sound2':'Boing','sound3':'Error digital','sound4':'Suspense','sound5':'Redoble rápido','sound6':'Aplausos','sound7':'Grillos','sound8':'Pequeña fanfarria','savedImage':'Archivo listo para compartir.','delete':'Eliminar paquete','removePhoto':'Quitar foto','addFirst':'Crea un sticker para empezar.','packCount':'stickers','packExists':'Paquete actualizado.'},
};

void main() => runApp(const FiguFactoryApp());

class FiguFactoryApp extends StatefulWidget {
  const FiguFactoryApp({super.key});
  @override State<FiguFactoryApp> createState() => _FiguFactoryAppState();
}
class _FiguFactoryAppState extends State<FiguFactoryApp> {
  String locale = 'pt-BR';
  SharedPreferencesAsync? preferences;
  @override void initState() { super.initState(); _load(); }
  Future<void> _load() async {
    preferences = SharedPreferencesAsync();
    final saved = await preferences!.getString('language');
    if (saved != null && copy.containsKey(saved) && mounted) setState(() => locale = saved);
  }
  String t(String key) => copy[locale]?[key] ?? copy['pt-BR']![key] ?? key;
  Future<void> setLocale(String value) async { setState(() => locale = value); await preferences?.setString('language', value); }
  @override Widget build(BuildContext context) => MaterialApp(
    debugShowCheckedModeBanner: false,
    title: 'FiguFábrica — Sticker Maker',
    theme: ThemeData(colorScheme: ColorScheme.fromSeed(seedColor: violet, brightness: Brightness.light), scaffoldBackgroundColor: const Color(0xFFF7F6FB), useMaterial3: true, fontFamily: 'Roboto'),
    home: FactoryHome(locale: locale, t: t, onLocale: setLocale),
  );
}

class FactoryHome extends StatefulWidget {
  const FactoryHome({super.key, required this.locale, required this.t, required this.onLocale});
  final String locale;
  final String Function(String) t;
  final ValueChanged<String> onLocale;
  @override State<FactoryHome> createState() => _FactoryHomeState();
}
class _FactoryHomeState extends State<FactoryHome> {
  int tab = 0;
  Uint8List? photo;
  final caption = TextEditingController();
  final packName = TextEditingController();
  final picker = ImagePicker();
  final audio = AudioPlayer();
  List<Map<String, dynamic>> packs = [];
  Color textColor = Colors.white;
  final keyPreview = GlobalKey();
  @override void initState() { super.initState(); _loadPacks(); }
  @override void dispose() { caption.dispose(); packName.dispose(); audio.dispose(); super.dispose(); }
  String t(String key) => widget.t(key);
  Future<void> _loadPacks() async {
    final pref = SharedPreferencesAsync(); final raw = await pref.getString('packs');
    if (raw != null) { try { final list = jsonDecode(raw) as List; if (mounted) setState(() => packs = list.map((e) => Map<String,dynamic>.from(e)).toList()); } catch (_) {} }
  }
  Future<void> _persist() async => SharedPreferencesAsync().setString('packs', jsonEncode(packs));
  Future<void> _pick() async {
    final file = await picker.pickImage(source: ImageSource.gallery, imageQuality: 92, maxWidth: 1600);
    if (file == null) return; final bytes = await file.readAsBytes(); if (mounted) setState(() => photo = bytes);
  }
  Future<Uint8List?> _renderPng() async {
    if (photo == null && caption.text.trim().isEmpty) { _toast(t('needImage')); return null; }
    const size = 512.0;
    final recorder = ui.PictureRecorder(); final canvas = Canvas(recorder, const Rect.fromLTWH(0,0,size,size));
    if (photo != null) {
      final codec = await ui.instantiateImageCodec(photo!, targetWidth: 440, targetHeight: 390); final frame = await codec.getNextFrame();
      final img = frame.image; final scale = (440 / img.width < 375 / img.height) ? 440 / img.width : 375 / img.height;
      final width = img.width * scale, height = img.height * scale;
      canvas.drawImageRect(img, Rect.fromLTWH(0,0,img.width.toDouble(),img.height.toDouble()), Rect.fromLTWH((size-width)/2,25+(390-height)/2,width,height), Paint()..filterQuality=FilterQuality.high);
    }
    final text = caption.text.trim();
    if (text.isNotEmpty) {
      final painter = TextPainter(text: TextSpan(text:text,style: TextStyle(color:textColor,fontSize:text.length>20? fortyFour:58,fontWeight:FontWeight.w900,shadows: const [Shadow(color:Color(0x88000000),blurRadius:4,offset:Offset(0,2))])),textAlign:TextAlign.center,textDirection:TextDirection.ltr,maxLines:2);
      painter.layout(maxWidth: 470); final top = photo == null ? (size-painter.height)/2 : 405-painter.height/2;
      painter.paint(canvas,Offset((size-painter.width)/2,top));
    }
    final picture = recorder.endRecording(); final image = await picture.toImage(512,512); final data = await image.toByteData(format:ui.ImageByteFormat.png); image.dispose();
    return data?.buffer.asUint8List();
  }
  static const double fortyFour = 44;
  Future<void> _save() async {
    final png = await _renderPng(); if (png == null) return;
    final name = packName.text.trim().isEmpty ? t('packs') : packName.text.trim();
    final index = packs.indexWhere((p) => (p['name'] as String).toLowerCase() == name.toLowerCase());
    final entry = {'png':base64Encode(png),'caption':caption.text.trim()};
    if (index<0) { packs.insert(0, {'name':name,'items':[entry]}); } else { (packs[index]['items'] as List).add(entry); }
    await _persist(); if (mounted) { setState(() {}); _toast(t('saved')); }
  }
  Future<void> _share() async {
    final png = await _renderPng(); if (png == null) return;
    await SharePlus.instance.share(ShareParams(files:[XFile.fromData(png,name:'figufabrica.png',mimeType:'image/png')],text:'FiguFábrica'));
  }
  void _toast(String message) { if (!mounted) return; ScaffoldMessenger.of(context).showSnackBar(SnackBar(content:Text(message),behavior:SnackBarBehavior.floating)); }
  Future<void> _play(int i) async { await audio.stop(); await audio.play(AssetSource('sounds/effect${i+1}.wav')); }
  Future<void> _changeLanguage() async {
    final selected = await showModalBottomSheet<String>(context:context,showDragHandle:true,builder:(context)=>SafeArea(child:Column(mainAxisSize:MainAxisSize.min,children:[Padding(padding:const EdgeInsets.all(16),child:Text(t('chooseLanguage'),style:Theme.of(context).textTheme.titleMedium)),...languages.entries.map((entry)=>ListTile(title:Text(entry.value),trailing:widget.locale==entry.key?const Icon(Icons.check,color:violet):null,onTap:()=>Navigator.pop(context,entry.key))),const SizedBox(height:8)])));
    if(selected!=null) widget.onLocale(selected);
  }
  @override Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title:Row(children:[Container(width:35,height:35,decoration:BoxDecoration(color:violet,borderRadius:BorderRadius.circular(12)),child:const Icon(Icons.auto_awesome,color:Colors.white,size:21)),const SizedBox(width:10),Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Text(t('brand'),style:const TextStyle(fontWeight:FontWeight.w800,fontSize:17)),Text(t('tag'),style:TextStyle(fontSize:10,color:Theme.of(context).colorScheme.onSurfaceVariant))])]),actions:[IconButton(onPressed:_changeLanguage,tooltip:t('settings'),icon:const Icon(Icons.translate)),const SizedBox(width:8)]),
    body:SafeArea(child:IndexedStack(index:tab,children:[_editor(),_packs(),_sounds()])),
    bottomNavigationBar:NavigationBar(selectedIndex:tab,onDestinationSelected:(v)=>setState(()=>tab=v),destinations:[NavigationDestination(icon:const Icon(Icons.edit_outlined),selectedIcon:const Icon(Icons.edit),label:t('create')),NavigationDestination(icon:const Icon(Icons.folder_outlined),selectedIcon:const Icon(Icons.folder),label:t('packs')),NavigationDestination(icon:const Icon(Icons.graphic_eq),label:t('sounds'))]),
  );
  Widget _editor() => SingleChildScrollView(
    padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(t('headline'), style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800, color: ink, letterSpacing: -.6)),
        const SizedBox(height: 5),
        Text(t('intro'), style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: Colors.black54)),
        const SizedBox(height: 17),
        _previewCard(),
        const SizedBox(height: 15),
        Wrap(
          spacing: 10,
          runSpacing: 8,
          children: [
            FilledButton.icon(onPressed: _pick, icon: const Icon(Icons.add_photo_alternate_outlined), label: Text(t('choosePhoto'))),
            if (photo != null) OutlinedButton.icon(onPressed: () => setState(() => photo = null), icon: const Icon(Icons.close), label: Text(t('removePhoto'))),
          ],
        ),
        const SizedBox(height: 15),
        TextField(
          controller: caption,
          onChanged: (_) => setState(() {}),
          maxLength: 40,
          decoration: InputDecoration(labelText: t('caption'), hintText: t('captionHint'), filled: true, fillColor: Colors.white, border: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide.none), counterText: ''),
        ),
        const SizedBox(height: 12),
        LayoutBuilder(builder: (context, constraints) {
          final colors = Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(t('textColor'), style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700)),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                children: [Colors.white, const Color(0xFFFFCB56), const Color(0xFFFF6A72), ink].map((color) => GestureDetector(
                  onTap: () => setState(() => textColor = color),
                  child: Container(width: 30, height: 30, decoration: BoxDecoration(color: color, shape: BoxShape.circle, border: Border.all(color: textColor == color ? violet : Colors.black12, width: textColor == color ? 3 : 1))),
                )).toList(),
              ),
            ],
          );
          final name = TextField(
            controller: packName,
            decoration: InputDecoration(labelText: t('packName'), hintText: t('packHint'), filled: true, fillColor: Colors.white, border: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide.none)),
          );
          if (constraints.maxWidth < 360) return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [colors, const SizedBox(height: 14), name]);
          return Row(crossAxisAlignment: CrossAxisAlignment.center, children: [Expanded(child: colors), const SizedBox(width: 16), Expanded(child: name)]);
        }),
        const SizedBox(height: 15),
        Wrap(
          spacing: 10,
          runSpacing: 10,
          children: [
            FilledButton.icon(onPressed: _save, icon: const Icon(Icons.bookmark_add_outlined), label: Text(t('save'))),
            OutlinedButton.icon(onPressed: _share, icon: const Icon(Icons.share_outlined), label: Text(t('share'))),
          ],
        ),
        const SizedBox(height: 12),
        Center(child: Text(t('privacy'), style: const TextStyle(color: Colors.black45, fontSize: 11))),
      ],
    ),
  );

  Widget _previewCard() => Container(
    key: keyPreview,
    width: double.infinity,
    padding: const EdgeInsets.all(14),
    decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(22), border: Border.all(color: const Color(0xFFE9E7F0))),
    child: Column(
      children: [
        Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
          Text(t('preview'), style: const TextStyle(fontWeight: FontWeight.w800)),
          Flexible(child: Text('512 × 512  •  ${t('transparent')}', textAlign: TextAlign.end, style: const TextStyle(fontSize: 10, color: Colors.black45))),
        ]),
        const SizedBox(height: 10),
        AspectRatio(
          aspectRatio: 1,
          child: Container(
            decoration: BoxDecoration(borderRadius: BorderRadius.circular(16), color: const Color(0xFFF7F6FB)),
            clipBehavior: Clip.antiAlias,
            child: Stack(
              alignment: Alignment.center,
              children: [
                if (photo != null) Padding(padding: const EdgeInsets.all(14), child: Image.memory(photo!, fit: BoxFit.contain)),
                if (caption.text.trim().isNotEmpty)
                  Positioned(
                    bottom: photo == null ? 0 : 18,
                    left: 14,
                    right: 14,
                    child: Text(caption.text, textAlign: TextAlign.center, maxLines: 2, style: TextStyle(fontSize: caption.text.length > 20 ? 26 : 32, fontWeight: FontWeight.w900, color: textColor, shadows: const [Shadow(color: Colors.black54, blurRadius: 4, offset: Offset(1, 2))])),
                  ),
                if (photo == null && caption.text.trim().isEmpty)
                  Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                    const Icon(Icons.auto_awesome, color: Color(0xFFC4B9FF), size: 34),
                    const SizedBox(height: 8),
                    Text(t('addFirst'), textAlign: TextAlign.center, style: const TextStyle(color: Colors.black45, fontSize: 12)),
                  ]),
              ],
            ),
          ),
        ),
      ],
    ),
  );

  Widget _packs() {
    if (packs.isEmpty) {
      return Center(child: Padding(padding: const EdgeInsets.all(30), child: Column(mainAxisSize: MainAxisSize.min, children: [const Icon(Icons.folder_open, color: violet, size: 48), const SizedBox(height: 12), Text(t('emptyPacks'), textAlign: TextAlign.center)])));
    }
    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: packs.length,
      itemBuilder: (context, index) {
        final pack = packs[index];
        final items = (pack['items'] as List).cast<Map>();
        return Card(
          color: Colors.white,
          child: ExpansionTile(
            title: Text(pack['name'], style: const TextStyle(fontWeight: FontWeight.w700)),
            subtitle: Text('${items.length} ${t('packCount')}'),
            leading: const CircleAvatar(backgroundColor: Color(0xFFEDE9FF), child: Icon(Icons.folder, color: violet)),
            trailing: IconButton(tooltip: t('delete'), onPressed: () async { setState(() => packs.removeAt(index)); await _persist(); }, icon: const Icon(Icons.delete_outline)),
            children: [
              if (items.isEmpty) Padding(padding: const EdgeInsets.all(14), child: Text(t('noStickers')))
              else Wrap(spacing: 8, runSpacing: 8, children: items.take(12).map((item) => Container(width: 64, height: 64, padding: const EdgeInsets.all(4), decoration: BoxDecoration(color: const Color(0xFFF7F6FB), borderRadius: BorderRadius.circular(12)), child: Image.memory(base64Decode(item['png']), fit: BoxFit.contain))).toList()),
              const SizedBox(height: 12),
            ],
          ),
        );
      },
      separatorBuilder: (_, __) => const SizedBox(height: 4),
    );
  }

  Widget _sounds() => ListView(
    padding: const EdgeInsets.all(16),
    children: [
      Text(t('soundIntro'), style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
      const SizedBox(height: 12),
      ...List.generate(8, (index) => Card(
        color: Colors.white,
        child: ListTile(
          leading: const CircleAvatar(backgroundColor: Color(0xFFEDE9FF), child: Icon(Icons.graphic_eq, color: violet)),
          title: Text(t('sound${index + 1}')),
          trailing: IconButton(onPressed: () => _play(index), icon: const Icon(Icons.play_circle_fill, color: violet, size: 30)),
        ),
      )),
      const SizedBox(height: 10),
      Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(color: const Color(0xFFEDE9FF), borderRadius: BorderRadius.circular(15)),
        child: Text(t('soundLimit'), style: const TextStyle(fontSize: 12, height: 1.45)),
      ),
    ],
  );
}
