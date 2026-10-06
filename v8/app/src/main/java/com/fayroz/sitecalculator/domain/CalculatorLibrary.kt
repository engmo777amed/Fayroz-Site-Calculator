package com.fayroz.sitecalculator.domain

import com.fayroz.sitecalculator.core.*
import kotlin.math.*

data class CalcField(val key:String,val label:String,val unit:String="",val default:String="",val required:Boolean=true,val signed:Boolean=false)
data class CalcOutput(val label:String,val value:Double,val unit:String)
data class CalcAnswer(val outputs:List<CalcOutput>,val explanation:String,val cost:Double=0.0,val consumedCost:Double=cost)
data class CalcDef(val id:String,val title:String,val group:String,val fields:List<CalcField>,val formula:String,
    val solve:(Map<String,Double>)->CalcAnswer)
object CalculatorLibrary {
    private fun f(k:String,l:String,u:String="",d:String="",required:Boolean=true,signed:Boolean=false)=CalcField(k,l,u,d,required,signed)
    private fun o(l:String,v:Double,u:String)=CalcOutput(l,v,u)
    private fun waste()=f("waste","الهالك","%","5",false)
    private fun price()=f("price","سعر وحدة الشراء","جنيه","0",false)
    private val area=f("area","المساحة الصافية","م²")
    private val length=f("length","الطول","م")
    private val width=f("width","العرض","م")
    private val height=f("height","الارتفاع","م")
    private val count=f("count","العدد","عدد","1")
    private fun get(v:Map<String,Double>,k:String)=v[k]?:0.0
    private operator fun Map<String,Double>.invoke(k:String)=get(this,k)
    val groups=listOf("المونة والمحارة","الأرضيات والكسوات","الدهانات والعزل","الجبس والأسقف","أدوات الموقع","المباني","الخرسانة والشدات","حصر الحديد","الأعمال الترابية","الطرق والرصف","حصر السباكة","حصر الكهرباء")
    val all:List<CalcDef> = buildList {
        fun addDef(id:String,title:String,g:Int,fields:List<CalcField>,formula:String,solve:(Map<String,Double>)->CalcAnswer){add(CalcDef(id,title,groups[g],fields,formula,solve))}
        fun mortar(id:String,title:String,thick:String,sand:String){
            addDef(id,title,0,listOf(area,f("thickness","متوسط السمك","مم",thick),f("cement","أسمنت في الخلطة","جزء","1"),f("sand","رمل في الخلطة","جزء",sand),f("dry","معامل الحجم الجاف","معامل","1.33"),f("density","كثافة الأسمنت الحجمية","كجم/م³","1440"),f("bag","وزن الشيكارة","كجم","50"),waste(),f("cementPrice","سعر شيكارة الأسمنت","جنيه","0",false),f("sandPrice","سعر متر الرمل","جنيه/م³","0",false),f("extraRate","معدل الإضافة","وحدة/م²","0",false),f("extraPrice","سعر وحدة الإضافة","جنيه","0",false)),
                "حجم منفذ = المساحة × السمك؛ الحجم الجاف = المنفذ × المعامل × (1 + الهالك/100). الأسمنت = متر الرمل × الشكاير لكل متر × وزن الشيكارة؛ ناتج المونة تقديري حسب معامل الحجم الجاف."){v->
                val wet=v("area")*v("thickness")/1000;val dry=wet*v("dry")*(1+v("waste")/100)
                val kg=dry*v("cement")/(v("cement")+v("sand"))*v("density");val sandM=dry*v("sand")/(v("cement")+v("sand"))
                val bags=ceil(kg/v("bag")-1e-9);val extra=v("area")*v("extraRate")*(1+v("waste")/100)
                CalcAnswer(listOf(o("مونة منفذة",wet,"م³"),o("أسمنت فعلي",kg,"كجم"),o("شراء أسمنت",bags,"شيكارة"),o("رمل",sandM,"م³"),o("مادة إضافية",extra,"وحدة"),o("تكلفة الاستهلاك",kg/v("bag")*v("cementPrice")+sandM*v("sandPrice")+extra*v("extraPrice"),"جنيه")),"عدد شكاير على متر رمل، والسمك متوسط التنفيذ. الشكاير تُقرب بعد تجميع الكمية.",bags*v("cementPrice")+sandM*v("sandPrice")+extra*v("extraPrice"),kg/v("bag")*v("cementPrice")+sandM*v("sandPrice")+extra*v("extraPrice"))
            }
        }
        mortar("plaster","مونة المحارة","15","4")
        mortar("splash","مونة الطرطشة","5","2")
        mortar("screed","مونة تسوية الأرضيات","50","4")
        mortar("bedding","مونة تركيب البلاط","30","4")
        mortar("custom_mortar","خلطة مونة مخصصة","15","4")
        addDef("faces","بياض أعمدة وكمرات وواجهات",0,listOf(length,height,count,f("faces","عدد الأوجه المتماثلة","وجه","1"),f("deduct","خصم فتحات وأجزاء","م²","0",false),price()),"المساحة = طول الوجه × ارتفاعه × الأوجه × العدد − الخصم."){v->val a=v("length")*v("height")*v("faces")*v("count")-v("deduct");require(a>=0){"الخصم أكبر من المساحة"};CalcAnswer(listOf(o("صافي البياض",a,"م²")),"اجمع حسابات منفصلة للأوجه المختلفة في المقاس.",a*v("price"))}
        addDef("tile","بلاط ورخام وجرانيت وكراتين",1,listOf(area,f("tileW","عرض القطعة","سم","60"),f("tileH","طول القطعة","سم","60"),f("pack","قطع في العبوة","قطعة","4"),waste(),f("price","سعر العبوة","جنيه","0",false)),"القطع = تقريب لأعلى (مساحة الشراء ÷ مساحة القطعة)؛ العبوات = تقريب لأعلى (القطع ÷ قطع العبوة)."){v->
            val need=v("area")*(1+v("waste")/100);val piece=v("tileW")*v("tileH")/10000;val pieces=ceil(need/piece);val packs=ceil(pieces/v("pack"));val buy=packs*v("pack")*piece
            CalcAnswer(listOf(o("صافي التنفيذ",v("area"),"م²"),o("بعد الهالك",need,"م²"),o("القطع المطلوبة",pieces,"قطعة"),o("عبوات الشراء",packs,"عبوة"),o("مساحة الشراء",buy,"م²"),o("فائض فوق المطلوب بالهالك",buy-need,"م²")),"مقاسات القطعة بالسنتيمتر؛ سعر العبوة الكاملة.",packs*v("price"),need/(v("pack")*piece)*v("price"))
        }
        addDef("skirting","وزرات بالقطع الجاهزة",1,listOf(length,f("piece","طول قطعة الوزرة","م","0.6"),f("pack","قطع بالعبوة","قطعة","1"),waste(),f("price","سعر العبوة","جنيه","0",false)),"القطع = الطول بعد الهالك ÷ طول القطعة؛ العبوات تقرب لأعلى."){v->val pieces=ceil(v("length")*(1+v("waste")/100)/v("piece"));val packs=ceil(pieces/v("pack"));CalcAnswer(listOf(o("قطع",pieces,"قطعة"),o("عبوات",packs,"عبوة")),"خصم عروض الأبواب من الطول قبل الإدخال.",packs*v("price"),v("length")*(1+v("waste")/100)/(v("piece")*v("pack"))*v("price"))}
        addDef("skirting_cut","تقطيع وزرات من البلاط",1,listOf(length,f("tileL","طول البلاطة","سم","60"),f("tileW","عرض البلاطة","سم","60"),f("strip","ارتفاع الوزرة","سم","10"),f("kerf","سمك القطع","مم","3",false),waste(),f("price","سعر البلاطة","جنيه","0",false)),"شرائح البلاطة = الجزء الصحيح ((العرض + القطع) ÷ (ارتفاع الوزرة + القطع))."){v->val kerf=v("kerf")/10;val strips=floor((v("tileW")+kerf)/(v("strip")+kerf));require(strips>=1){"مقاس الوزرة أكبر من البلاطة"};val per=strips*v("tileL")/100;val tiles=ceil(v("length")*(1+v("waste")/100)/per);CalcAnswer(listOf(o("شرائح من البلاطة",strips,"شريحة"),o("طول من البلاطة",per,"م ط"),o("بلاطات شراء",tiles,"بلاطة")),"اتجاه تقطيع واحد؛ يمكن مقارنة الاتجاه الآخر بتبديل أبعاد البلاطة.",tiles*v("price"),v("length")*(1+v("waste")/100)/per*v("price"))}
        addDef("stairs_finish","كسوة درجات وبسطات السلالم",1,listOf(f("steps","عدد الدرجات","درجة"),width,f("tread","النائمة","سم","30"),f("riser","القائمة","سم","17"),f("landing","مساحة البسطات","م²","0",false),waste(),price()),"الكسوة = عرض السلم × الدرجات × (النائمة + القائمة) + البسطات."){v->val tread=v("width")*v("steps")*v("tread")/100;val rise=v("width")*v("steps")*v("riser")/100;val a=tread+rise+v("landing");val buy=a*(1+v("waste")/100);CalcAnswer(listOf(o("النوايم",tread,"م²"),o("القوايم",rise,"م²"),o("البسطات",v("landing"),"م²"),o("صافي الكسوة",a,"م²"),o("شراء بالهالك",buy,"م²")),"البروز والحواف والوزرات تُحصر مستقلة.",buy*v("price"))}
        fun consumable(id:String,title:String,g:Int,rate:String,pack:String,unit:String){
            addDef(id,title,g,listOf(area,f("rate","الاستهلاك للوجه / الطبقة","$unit/م²",rate),f("coats","عدد الأوجه / الطبقات","عدد","1"),f("pack","حجم العبوة",unit,pack),waste(),f("price","سعر العبوة","جنيه","0",false)),"الكمية = المساحة × معدل الوجه × عدد الأوجه × (1 + الهالك/100). إذا المعدل للنظام كاملًا اجعل العدد 1."){v->val need=v("area")*v("rate")*v("coats")*(1+v("waste")/100);val packs=ceil(need/v("pack"));CalcAnswer(listOf(o("كمية فعلية",need,unit),o("عبوات كاملة",packs,"عبوة"),o("كمية شراء",packs*v("pack"),unit),o("فائض التعبئة",packs*v("pack")-need,unit)),"المعدل المدخل للوجه الواحد؛ نشرة المنتج تحدد المعدل.",packs*v("price"),need/v("pack")*v("price"))}
        }
        consumable("adhesive","لاصق بلاط أو بلوك",1,"5","20","كجم")
        addDef("grout","روبة فواصل البلاط",1,listOf(area,f("tileL","طول البلاطة","مم","600"),f("tileW","عرض البلاطة","مم","600"),f("jointW","عرض الفاصل","مم","3"),f("jointD","عمق الفاصل","مم","8"),f("density","كثافة الروبة","كجم/لتر","1.6"),f("pack","وزن العبوة","كجم","5"),waste(),f("price","سعر العبوة","جنيه","0",false)),"كجم/م² = (الطول + العرض) ÷ (الطول × العرض) × عرض الفاصل × العمق × الكثافة."){v->val rate=(v("tileL")+v("tileW"))/(v("tileL")*v("tileW"))*v("jointW")*v("jointD")*v("density");val kg=rate*v("area")*(1+v("waste")/100);val packs=ceil(kg/v("pack"));CalcAnswer(listOf(o("معدل تقريبي",rate,"كجم/م²"),o("روبة",kg,"كجم"),o("عبوات",packs,"عبوة")),"معادلة تقديرية؛ راجع معدل الشركة لشكل البلاط والفاصل.",packs*v("price"),kg/v("pack")*v("price"))}
        consumable("putty","معجون حوائط وأسقف",2,"1","25","كجم")
        consumable("primer","بطانة دهانات",2,"0.1","10","لتر")
        addDef("paint","دهان نهائي أو واجهات",2,listOf(area,f("coverage","تغطية الوجه","م²/لتر","10"),f("coats","عدد الأوجه","عدد","2"),f("pack","حجم العبوة","لتر","10"),waste(),f("price","سعر العبوة","جنيه","0",false)),"اللترات = المساحة × الأوجه ÷ التغطية × (1 + الهالك/100)."){v->val qty=v("area")*v("coats")/v("coverage")*(1+v("waste")/100);val packs=ceil(qty/v("pack"));CalcAnswer(listOf(o("استهلاك دهان",qty,"لتر"),o("عبوات",packs,"عبوة"),o("شراء",packs*v("pack"),"لتر")),"التغطية لكل وجه؛ البطانة والمعجون حسابات منفصلة.",packs*v("price"),qty/v("pack")*v("price"))}
        consumable("woodpaint","دهان أخشاب ومعادن",2,"0.12","3","لتر")
        consumable("waterproof","عزل أسمنتي أو دهان",2,"1.5","20","كجم")
        addDef("rolls","لفائف عزل وتراكبات",2,listOf(area,f("perimeter","محيط رجوع العزل","م","0",false),f("upstand","ارتفاع الرجوع","م","0.2",false),f("rollL","طول اللفة","م","10"),f("rollW","عرض اللفة","م","1"),f("overlapL","تراكب النهاية","سم","15",false),f("overlapW","تراكب الجانب","سم","10",false),waste(),f("price","سعر اللفة","جنيه","0",false)),"التغطية الفعالة للفة = (الطول − تراكب النهاية) × (العرض − تراكب الجانب)."){v->val effective=(v("rollL")-v("overlapL")/100)*(v("rollW")-v("overlapW")/100);require(v("rollL")>v("overlapL")/100&&v("rollW")>v("overlapW")/100){"التراكب أكبر من مقاس اللفة"};val a=(v("area")+v("perimeter")*v("upstand"))*(1+v("waste")/100);val rolls=ceil(a/effective);CalcAnswer(listOf(o("مساحة بالرجوع والهالك",a,"م²"),o("تغطية فعالة للفة",effective,"م²"),o("لفات",rolls,"لفة")),"تقدير شراء محافظ؛ راجع اتجاه الفرد والتفاصيل الفعلية.",rolls*v("price"),a/effective*v("price"))}
        addDef("insulation","ألواح عزل حراري",2,listOf(area,f("boardL","طول اللوح","م","1.2"),f("boardW","عرض اللوح","م","0.6"),f("thickness","السمك","سم","5"),waste(),f("price","سعر اللوح","جنيه","0",false)),"الألواح = المساحة بالهالك ÷ مساحة اللوح؛ الحجم = مساحة الشراء × السمك."){v->val pieces=ceil(v("area")*(1+v("waste")/100)/(v("boardL")*v("boardW")));CalcAnswer(listOf(o("ألواح",pieces,"لوح"),o("حجم شراء",pieces*v("boardL")*v("boardW")*v("thickness")/100,"م³")),"السمك يحدد مواصفة اللوح وليس مساحة التغطية.",pieces*v("price"),v("area")*(1+v("waste")/100)/(v("boardL")*v("boardW"))*v("price"))}
        addDef("sealant","مادة ملء الفواصل",2,listOf(length,f("jointW","عرض الفاصل","مم","10"),f("jointD","عمق الملء","مم","10"),f("pack","حجم العبوة","مل","300"),waste(),f("price","سعر العبوة","جنيه","0",false)),"مل = طول بالمتر × عرض بالمليمتر × عمق بالمليمتر."){v->val ml=v("length")*v("jointW")*v("jointD")*(1+v("waste")/100);val packs=ceil(ml/v("pack"));CalcAnswer(listOf(o("استهلاك",ml,"مل"),o("عبوات",packs,"عبوة")),"عمق الملء الفعلي بعد تركيب الحبل الخلفي إن وجد.",packs*v("price"),ml/v("pack")*v("price"))}
        addDef("gypsum","ألواح جبس / قواطيع / مستويات",3,listOf(area,f("vertical","مساحة الجوانب الرأسية","م²","0",false),f("layers","عدد طبقات الألواح","طبقة","1"),f("sides","عدد الأوجه المغطاة","وجه","1"),f("boardL","طول اللوح","م","2.4"),f("boardW","عرض اللوح","م","1.2"),waste(),f("price","سعر اللوح","جنيه","0",false)),"الألواح = (المسطح + الجوانب) × الطبقات × الأوجه × الهالك ÷ مساحة اللوح."){v->val buy=(v("area")+v("vertical"))*v("layers")*v("sides")*(1+v("waste")/100);val boards=ceil(buy/(v("boardL")*v("boardW")));CalcAnswer(listOf(o("مساحة تغطية بالهالك",buy,"م²"),o("ألواح",boards,"لوح")),"للقواطيع أدخل وجهًا واحدًا ثم عدد الأوجه 2. ملحقات النظام منفصلة.",boards*v("price"),buy/(v("boardL")*v("boardW"))*v("price"))}
        addDef("gypsum_system","قطاعات وعلاقات ومسامير الجبس",3,listOf(area,f("profiles","معدل القطاعات","م ط/م²"),f("profileL","طول القطاع","م","3"),f("hangers","معدل العلاقات","عدد/م²"),f("screws","معدل المسامير","عدد/م²"),waste(),f("profilePrice","سعر القطاع","جنيه","0",false),f("hangerPrice","سعر العلاقة","جنيه","0",false),f("screwPrice","سعر المسمار","جنيه","0",false)),"كل ملحق = المساحة × معدل نظام التركيب المدخل × الهالك."){v->val a=v("area")*(1+v("waste")/100);val sections=ceil(a*v("profiles")/v("profileL"));val h=ceil(a*v("hangers"));val screws=ceil(a*v("screws"));CalcAnswer(listOf(o("قطاعات",sections,"قطعة"),o("علاقات",h,"عدد"),o("مسامير",screws,"عدد")),"أدخل معدلات نظام الشركة المختار؛ لا توجد معدلات موحدة لكل الأنظمة.",sections*v("profilePrice")+h*v("hangerPrice")+screws*v("screwPrice"))}
        addDef("light_cove","جوانب وبيوت نور",3,listOf(length,f("developed","العرض المفرود للأوجه","م"),waste(),price()),"مساحة الأوجه = طول المسار × مجموع عروض الأوجه."){v->val net=v("length")*v("developed");val buy=net*(1+v("waste")/100);CalcAnswer(listOf(o("صافي أوجه",net,"م²"),o("بالقص والهالك",buy,"م²")),"أدخل مجموع عروض الجوانب والأوجه المطلوب تغطيتها.",buy*v("price"))}
        addDef("slope","منسوب نهاية من الميل",4,listOf(length,f("start","منسوب البداية","م","0",false,true),f("slope","الميل الصاعد موجب والنازل سالب","%","-1",false,true)),"فرق المنسوب = الطول الأفقي × الميل/100؛ النهاية = البداية + الفرق."){v->val diff=v("length")*v("slope")/100;CalcAnswer(listOf(o("فرق المنسوب",diff,"م"),o("منسوب النهاية",v("start")+diff,"م"),o("ميل لكل متر",v("slope"),"سم/م"),o("نسبة 1 إلى",if(v("slope")!=0.0)100/abs(v("slope")) else 0.0,"")),"الطول أفقي؛ الميل السالب نزول والموجب صعود. صفر يعني أفقي.")}
        addDef("levels","ميل من منسوبين",4,listOf(length,f("start","منسوب البداية","م","0",false,true),f("end","منسوب النهاية","م","0",false,true)),"الميل = (النهاية − البداية) ÷ الطول الأفقي × 100."){v->val diff=v("end")-v("start");val slope=diff/v("length")*100;CalcAnswer(listOf(o("فرق المنسوب",diff,"م"),o("الميل الموجّه",slope,"%"),o("ميل لكل متر",slope,"سم/م")),"الإشارة الموجبة صعود والسالبة نزول.")}
        addDef("floor_slope","ميول أرضية إلى صفاية",4,listOf(length,f("drain","منسوب التشطيب عند الصفاية","م","0",false,true),f("slope","الميل نحو الصفاية","%","1"),area,f("minThickness","سمك المونة عند الصفاية","سم","2")),"فرق المنسوب = طول مسار الميل × الميل/100؛ تقدير المونة بمتوسط سمك طرفي مسار مستوٍ."){v->val d=v("length")*v("slope")/100;val avg=v("minThickness")/100+d/2;CalcAnswer(listOf(o("منسوب الطرف البعيد",v("drain")+d,"م"),o("فرق المنسوب",d*100,"سم"),o("سمك الطرف البعيد",v("minThickness")+d*100,"سم"),o("حجم مونة تقديري",v("area")*avg,"م³")),"ينطبق على مسطح ذي ميل خطي وقاعدة أفقية؛ قسّم الأرضية ذات الميول المتعددة لأجزاء.")}
        addDef("rectangle","مساحة مستطيل",4,listOf(length,width,count,f("deduct","خصم","م²","0",false)),"الطول × العرض × العدد − الخصم."){v->val a=v("length")*v("width")*v("count")-v("deduct");require(a>=0);CalcAnswer(listOf(o("مساحة صافية",a,"م²")),"الخصم لكل مجموع الحساب.")}
        addDef("triangle","مساحة مثلث",4,listOf(f("base","القاعدة","م"),height,count),"نصف القاعدة × الارتفاع العمودي × العدد."){v->CalcAnswer(listOf(o("المساحة",v("base")*v("height")/2*v("count"),"م²")),"الارتفاع عمودي على القاعدة.")}
        addDef("circle","دائرة أو حلقة",4,listOf(f("diameter","القطر الخارجي","م"),f("inner","قطر داخلي للخصم","م","0",false)),"المساحة = π/4 × (القطر الخارجي² − الداخلي²)."){v->require(v("diameter")>=v("inner"));CalcAnswer(listOf(o("المساحة",PI/4*(v("diameter").pow(2)-v("inner").pow(2)),"م²"),o("محيط خارجي",PI*v("diameter"),"م")),"اترك القطر الداخلي صفرًا للدائرة المصمتة.")}
        addDef("trapezoid","مساحة شبه منحرف",4,listOf(f("a","الضلع الموازي الأول","م"),f("b","الضلع الموازي الثاني","م"),height),"المساحة = (مجموع الضلعين المتوازيين)/2 × الارتفاع العمودي."){v->CalcAnswer(listOf(o("المساحة",(v("a")+v("b"))/2*v("height"),"م²")),"الارتفاع هو المسافة العمودية بين الضلعين.")}
        addDef("layer","حجم طبقة من المساحة والسمك",4,listOf(area,f("thickness","متوسط السمك","سم"),waste(),f("density","كثافة اختيارية","طن/م³","0",false),price()),"الحجم = المساحة × السمك/100؛ الوزن = حجم التوريد × الكثافة المدخلة."){v->val net=v("area")*v("thickness")/100;val buy=net*(1+v("waste")/100);CalcAnswer(listOf(o("حجم منفذ",net,"م³"),o("حجم توريد",buy,"م³"),o("وزن عند الكثافة المدخلة",buy*v("density"),"طن")),"سعر الوحدة هنا للمتر المكعب؛ الوزن يظهر فقط عند إدخال كثافة.",buy*v("price"))}
        addDef("diagonal","وتر وقطر مستطيل",4,listOf(length,width),"الوتر = الجذر التربيعي (الطول² + العرض²)."){v->CalcAnswer(listOf(o("الوتر",hypot(v("length"),v("width")),"م")),"ضلعان متعامدان.")}
        addDef("spacing","تقسيم مسافة بالتساوي",4,listOf(length,f("intervals","عدد المسافات","مسافة")),"المسافة = الطول ÷ عدد المسافات؛ نقاط تشمل الطرفين = المسافات + 1."){v->CalcAnswer(listOf(o("المسافة",v("length")/v("intervals"),"م"),o("عدد النقاط",v("intervals")+1,"نقطة")),"عدد المسافات مختلف عن عدد النقاط.")}
        addDef("reverse_mortar","الأسمنت الموجود يكفي كام محارة؟",4,listOf(f("bags","شكاير متاحة","شيكارة"),f("bag","وزن الشيكارة","كجم","50"),f("thickness","سمك المحارة","مم","15"),f("cement","أسمنت","جزء","1"),f("sand","رمل","جزء","4"),f("dry","معامل جاف","معامل","1.33"),f("density","كثافة الأسمنت الحجمية","كجم/م³","1440"),waste()),"المساحة = الأسمنت المتاح ÷ استهلاك الأسمنت للمتر عند الخلطة والسمك المدخلين."){v->val rate=v("thickness")/1000*v("dry")*(1+v("waste")/100)*v("cement")/(v("cement")+v("sand"))*v("density");val a=v("bags")*v("bag")/rate;CalcAnswer(listOf(o("مساحة يمكن تنفيذها",a,"م²"),o("رمل مطلوب",a*v("thickness")/1000*v("dry")*(1+v("waste")/100)*v("sand")/(v("cement")+v("sand")),"م³")),"بشرط توافر الرمل وباقي المواد.")}
        addDef("reverse_coverage","عبوات الخامة تكفي كام؟",4,listOf(f("packs","عدد العبوات","عبوة"),f("pack","حجم العبوة","وحدة"),f("rate","استهلاك الوجه","وحدة/م²"),f("coats","عدد الأوجه","وجه","1"),waste()),"المساحة = الكمية المتاحة ÷ (استهلاك الوجه × عدد الأوجه × الهالك)."){v->CalcAnswer(listOf(o("مساحة متاحة",v("packs")*v("pack")/(v("rate")*v("coats")*(1+v("waste")/100)),"م²")),"الوحدة نفسها للعبوة ومعدل الاستهلاك.")}
        fun masonry(id:String,title:String){
            addDef(id,title,5,listOf(area,f("wall","سمك الحائط","سم",if(id=="blocks")""else "12"),f("brickL","طول الوحدة","سم",if(id=="blocks")""else "25"),f("brickW","عرض الوحدة","سم",if(id=="blocks")""else "12"),f("brickH","ارتفاع الوحدة","سم",if(id=="blocks")""else "6"),f("joint","سمك اللحام","مم","10",false),f("dry","معامل جاف","معامل","1.33"),f("cement","أسمنت المونة","جزء","1"),f("sand","رمل المونة","جزء","5"),waste(),f("brickPrice","سعر ألف وحدة","جنيه","0",false),f("cementPrice","سعر شيكارة 50 كجم","جنيه","0",false),f("sandPrice","سعر متر الرمل","جنيه","0",false)),"عدد الوحدات حسب وجه الرص وعدد الطبقات عبر السمك؛ المونة = حجم الحائط − الحجم الخارجي للوحدات."){v->
                val l=v("brickL")/100;val w=v("brickW")/100;val h=v("brickH")/100;val j=v("joint")/1000;val t=v("wall")/100
                val courses=max(1.0,round((t+j)/(w+j)));val net=v("area")/((l+j)*(h+j))*courses
                val wet=(v("area")*t-net*l*w*h).coerceAtLeast(0.0);val dry=wet*v("dry")
                val kg=dry*v("cement")/(v("cement")+v("sand"))*1440;val sm=dry*v("sand")/(v("cement")+v("sand"));val bricks=ceil(net*(1+v("waste")/100));val bags=ceil(kg/50-1e-9)
                CalcAnswer(listOf(o("طبقات عبر السمك",courses,"طبقة"),o("وحدات شراء",bricks,"وحدة"),o("مونة منفذة",wet,"م³"),o("أسمنت",bags,"شيكارة"),o("رمل",sm,"م³"),o("أسمنت فعلي",kg,"كجم")),"تقدير لرص طولي منتظم؛ عدد الطبقات المدعوم يطابق عرض الوحدة. للرص المختلف استخدم مقاس وجه الرص الفعلي. البلوك المجوف محسوب بالأبعاد الخارجية.",bricks/1000*v("brickPrice")+bags*v("cementPrice")+sm*v("sandPrice"),net*(1+v("waste")/100)/1000*v("brickPrice")+kg/50*v("cementPrice")+sm*v("sandPrice"))
            }
        }
        masonry("masonry","طوب أحمر ومونة")
        masonry("blocks","بلوك أسمنتي أو خفيف")
        consumable("block_adhesive","لاصق مباني بلوك",5,"4","25","كجم")
        fun box(id:String,title:String,g:Int){addDef(id,title,g,listOf(length,width,height,count,waste(),price()),"الحجم = الطول × العرض × الارتفاع × العدد؛ التوريد بإضافة الهالك."){v->val net=v("length")*v("width")*v("height")*v("count");val buy=net*(1+v("waste")/100);CalcAnswer(listOf(o("حجم صافي",net,"م³"),o("حجم توريد",buy,"م³")),"الأبعاد بالمتر؛ السعر للمتر المكعب.",buy*v("price"))}}
        box("footing","قواعد منفصلة أو شريطية",6);box("raft","حجم خرسانة مستطيلة — قواعد وبلاطات وكمرات وأعمدة",6);box("beam","كمرات أو أعتاب",6);box("column","أعمدة مستطيلة",6);box("wall_concrete","حوائط خرسانية",6)
        addDef("round_column","أعمدة دائرية",6,listOf(f("diameter","القطر","م"),height,count,waste(),price()),"الحجم = π × القطر²/4 × الارتفاع × العدد."){v->val net=PI*v("diameter").pow(2)/4*v("height")*v("count");val buy=net*(1+v("waste")/100);CalcAnswer(listOf(o("حجم صافي",net,"م³"),o("توريد",buy,"م³")),"الأعمدة الدائرية بالأقطار الفعلية.",buy*v("price"))}
        addDef("stairs_concrete","خرسانة سلم مستقيم",6,listOf(width,f("run","الإسقاط الأفقي للقلبة","م"),f("rise","ارتفاع القلبة","م"),f("waist","سمك بطنية السلم عموديًا عليها","سم","15"),f("steps","عدد النوايم","عدد"),f("tread","عرض النائمة","سم","30"),f("riser","ارتفاع القائمة","سم","17"),f("landingVolume","خرسانة البسطات","م³","0",false),waste(),price()),"البطنية = طول مائل × العرض × السمك؛ الدرجات المثلثة = نصف النائمة × القائمة × العرض × العدد."){v->val waist=hypot(v("run"),v("rise"))*v("width")*v("waist")/100;val steps=.5*v("tread")/100*v("riser")/100*v("width")*v("steps");val net=waist+steps+v("landingVolume");val buy=net*(1+v("waste")/100);CalcAnswer(listOf(o("بطنية",waist,"م³"),o("درجات",steps,"م³"),o("صافي",net,"م³"),o("توريد",buy,"م³")),"للقلبة المستقيمة؛ لا تخصم التداخلات مع البسطات تلقائيًا.",buy*v("price"))}
        addDef("form_column","شدة أعمدة وحوائط",6,listOf(length,width,height,count,f("faces","عدد الأوجه للحائط؛ 0 لمحيط العمود","وجه","0",false),price()),"العمود = 2 × (الطول + العرض) × الارتفاع؛ الحائط = الطول × الارتفاع × الأوجه."){v->val a=(if(v("faces")==0.0)2*(v("length")+v("width")) else v("length")*v("faces"))*v("height")*v("count");CalcAnswer(listOf(o("مساحة الشدة",a,"م²")),"لا يشمل قاع العنصر.",a*v("price"))}
        addDef("form_beam","شدة كمرات",6,listOf(length,width,height,count,price()),"شدة الكمرة = الطول × (العرض + 2 × ارتفاع الجوانب) × العدد."){v->val a=v("length")*(v("width")+2*v("height"))*v("count");CalcAnswer(listOf(o("مساحة شدة",a,"م²")),"أدخل ارتفاع الجوانب الفعلي أسفل البلاطة.",a*v("price"))}
        addDef("steel","وزن أسياخ الحديد",7,listOf(f("diameter","القطر","مم"),length,count,waste(),f("price","سعر الطن","جنيه","0",false)),"وزن المتر = القطر²/162؛ الوزن = وزن المتر × طول السيخ × العدد."){v->val per=v("diameter").pow(2)/162;val net=per*v("length")*v("count");val buy=net*(1+v("waste")/100);CalcAnswer(listOf(o("وزن المتر",per,"كجم/م"),o("وزن صافي",net,"كجم"),o("شراء",buy,"كجم")),"حصر أوزان فقط؛ المقاسات من اللوحات المعتمدة.",buy/1000*v("price"))}
        addDef("mesh","شبكات حديد باتجاهين",7,listOf(length,width,f("cover","غطاء لكل طرف","سم","2.5",false),f("spacing","المسافة القصوى بين الأسياخ","سم"),f("diameter","قطر الأسياخ","مم"),f("layers","عدد الشبكات","شبكة","1"),f("lap","إضافة لكل سيخ للتراكب","م","0",false),waste(),f("price","سعر الطن","جنيه","0",false)),"عدد الأسياخ = تقريب لأعلى (البعد الصافي ÷ المسافة القصوى) + 1."){v->val l=v("length")-2*v("cover")/100;val w=v("width")-2*v("cover")/100;require(l>0&&w>0);val nl=ceil(w/(v("spacing")/100))+1;val nw=ceil(l/(v("spacing")/100))+1;val total=(nl*(l+v("lap"))+nw*(w+v("lap")))*v("layers");val kg=total*v("diameter").pow(2)/162*(1+v("waste")/100);CalcAnswer(listOf(o("أسياخ اتجاه الطول للشبكة",nl,"سيخ"),o("أسياخ اتجاه العرض للشبكة",nw,"سيخ"),o("إجمالي أطوال",total,"م"),o("وزن شراء",kg,"كجم")),"أقطار ومسافات وتراكبات مُدخلة من اللوحة؛ ليس تصميم تسليح.",kg/1000*v("price"))}
        addDef("stirrups","حصر كانات",7,listOf(f("a","طول مركز السيخ بالكانة","سم"),f("b","عرض مركز السيخ بالكانة","سم"),f("hooks","إجمالي الخطافات والثنيات حسب التفريدة","سم"),f("diameter","قطر السيخ","مم"),count,f("price","سعر الطن","جنيه","0",false)),"طول الكانة = 2 × (الطول + العرض) + الإضافات حسب التفريدة المعتمدة."){v->val per=(2*(v("a")+v("b"))+v("hooks"))/100;val kg=per*v("count")*v("diameter").pow(2)/162;CalcAnswer(listOf(o("طول الكانة",per,"م"),o("إجمالي أطوال",per*v("count"),"م"),o("وزن",kg,"كجم")),"أبعاد على مركز السيخ وإضافات الثني والخطافات من التفريدة.",kg/1000*v("price"))}
        addDef("cutting","تقطيع أسياخ لمقاس متماثل",7,listOf(f("stock","طول السيخ التجاري","م","12"),f("piece","طول القطعة المطلوبة","م"),count,f("kerf","فاقد القطعة","مم","0",false),f("price","سعر السيخ","جنيه","0",false)),"قطع السيخ = الجزء الصحيح ((طول السيخ + القطع) ÷ (طول القطعة + القطع))."){v->val kerf=v("kerf")/1000;val per=floor((v("stock")+kerf)/(v("piece")+kerf));require(per>=1){"القطعة أطول من السيخ"};val bars=ceil(v("count")/per);val waste=bars*v("stock")-v("count")*v("piece");CalcAnswer(listOf(o("قطع لكل سيخ",per,"قطعة"),o("أسياخ شراء",bars,"سيخ"),o("فضلات وقص إجمالية",waste,"م")),"للمقاسات المتماثلة؛ ليس تحسينًا لمجموعة أطوال مختلفة.",bars*v("price"),v("count")*v("piece")/v("stock")*v("price"))}
        box("excavation","حفر قواعد وخنادق",8)
        addDef("fill","ردم أو إحلال بطبقات",8,listOf(area,f("thickness","السمك المنفذ","سم"),f("deduct","حجم منشآت مستبعدة","م³","0",false),f("factor","معامل توريد / منفذ","معامل","1"),f("truck","حجم حمولة السيارة","م³","10"),price()),"منفذ = المساحة × السمك − المستبعد؛ التوريد = المنفذ × المعامل المحدد."){v->val net=v("area")*v("thickness")/100-v("deduct");require(net>=0);val buy=net*v("factor");CalcAnswer(listOf(o("حجم منفذ",net,"م³"),o("حجم توريد",buy,"م³"),o("رحلات تقديرية",ceil(buy/v("truck")),"رحلة")),"معامل التوريد من بيانات المادة؛ راعِ حد الوزن الفعلي للسيارة.",buy*v("price"))}
        addDef("earth_levels","حجم بين منسوبين",8,listOf(area,f("start","المنسوب الحالي","م","0",false,true),f("end","المنسوب المطلوب","م","0",false,true),price()),"الحجم = المساحة × القيمة المطلقة لفرق المنسوب المنتظم."){v->val diff=v("end")-v("start");val volume=v("area")*abs(diff);CalcAnswer(listOf(o(if(diff>=0)"ردم" else "حفر",volume,"م³")),"للمناسيب المنتظمة؛ المسطحات المتغيرة تُقسم لأجزاء.",volume*v("price"))}
        addDef("asphalt","أسفلت وفرد الطن",9,listOf(area,f("thickness","السمك بعد الدمك","سم","5"),f("density","كثافة الخلطة بعد الدمك","طن/م³","2.431"),waste(),f("price","سعر الطن","جنيه","0",false)),"الأطنان = المساحة × السمك/100 × الكثافة؛ فرد الطن = 1 ÷ (السمك × الكثافة)."){v->val net=v("area")*v("thickness")/100*v("density");val buy=net*(1+v("waste")/100);CalcAnswer(listOf(o("أطنان منفذة",net,"طن"),o("أطنان توريد",buy,"طن"),o("فرد الطن",100/(v("thickness")*v("density")),"م²/طن"),o("تكلفة الخلطة للمتر",buy*v("price")/v("area"),"جنيه/م²")),"كثافة فعلية من تصميم الخلطة؛ تكلفة التوريد حسب سعر الطن المدخل.",buy*v("price"))}
        addDef("base_course","طبقات أساس وتحت أساس",9,listOf(area,f("thickness","سمك مدموك","سم"),f("factor","معامل توريد / مدموك","معامل","1"),f("density","كثافة التوريد","طن/م³"),f("price","سعر طن التوريد","جنيه","0",false)),"حجم مدموك = المساحة × السمك؛ توريد = الحجم المدموك × المعامل."){v->val net=v("area")*v("thickness")/100;val loose=net*v("factor");val tons=loose*v("density");CalcAnswer(listOf(o("حجم مدموك",net,"م³"),o("حجم توريد",loose,"م³"),o("وزن توريد",tons,"طن")),"الكثافة هنا لحالة التوريد؛ لا تستخدم كثافة الدمك على الحجم المفكك.",tons*v("price"))}
        addDef("spray","تشريب أو لصق MC / RC",9,listOf(area,f("rate","معدل الرش","كجم/م²"),f("density","كثافة المادة","كجم/لتر"),waste(),f("price","سعر الطن","جنيه","0",false)),"الوزن = المساحة × المعدل × الهالك؛ اللترات = الكجم ÷ الكثافة."){v->val kg=v("area")*v("rate")*(1+v("waste")/100);CalcAnswer(listOf(o("كمية",kg,"كجم"),o("حجم",kg/v("density"),"لتر")),"حدد إن كان معدل المواصفة للمادة المرشوشة أو للمتبقي؛ لا يتم التحويل بينهما تلقائيًا.",kg/1000*v("price"))}
        addDef("kerb","بردورات",9,listOf(length,f("piece","طول البردورة","م","1"),waste(),f("price","سعر القطعة","جنيه","0",false)),"عدد البردورات = الطول بالهالك ÷ طول القطعة، مقربًا لأعلى."){v->val pieces=ceil(v("length")*(1+v("waste")/100)/v("piece"));CalcAnswer(listOf(o("قطع",pieces,"قطعة")),"الأركان والقطع الخاصة تُحصر منفصلة.",pieces*v("price"),v("length")*(1+v("waste")/100)/v("piece")*v("price"))}
        consumable("marking","دهانات علامات الطريق",9,"0.5","20","كجم")
        addDef("interlock","إنترلوك وفرشة",9,listOf(area,f("pieceArea","مساحة قطعة الإنترلوك","م²"),f("bed","سمك الفرشة","سم","3"),waste(),f("price","سعر المتر المربع","جنيه","0",false)),"القطع = المساحة بالهالك ÷ مساحة القطعة؛ الفرشة = المساحة الصافية × السمك."){v->val buy=v("area")*(1+v("waste")/100);CalcAnswer(listOf(o("قطع شراء",ceil(buy/v("pieceArea")),"قطعة"),o("مساحة شراء",buy,"م²"),o("حجم فرشة",v("area")*v("bed")/100,"م³")),"طبقة الأساس حاسبة مستقلة.",buy*v("price"))}
        addDef("pipes","مواسير تغذية وصرف حسب القطر",10,listOf(length,f("piece","طول الماسورة التجاري","م","4"),waste(),f("price","سعر الماسورة","جنيه","0",false)),"المواسير = طول المسارات بالهالك ÷ طول الماسورة التجاري."){v->val buy=v("length")*(1+v("waste")/100);val pieces=ceil(buy/v("piece"));CalcAnswer(listOf(o("طول مطلوب",buy,"م"),o("مواسير شراء",pieces,"ماسورة"),o("فائض أطوال",pieces*v("piece")-buy,"م")),"اعمل حسابًا مستقلًا لكل قطر ونوع؛ القطع والوصلات حسب القائمة المسجلة.",pieces*v("price"),buy/v("piece")*v("price"))}
        addDef("fittings","قطع ووصلات وأجهزة بالعدد",10,listOf(count,f("waste","احتياطي شراء","%","0",false),price()),"شراء = العدد × الهالك، مقربًا لأعلى."){v->val buy=ceil(v("count")*(1+v("waste")/100));CalcAnswer(listOf(o("شراء",buy,"عدد")),"سجّل اسم القطعة والقطر في اسم الحساب؛ لا نستنتج الوصلات من الطول.",buy*v("price"))}
        addDef("pipe_insulation","عزل مواسير",10,listOf(length,f("diameter","القطر الخارجي للعزل","مم"),waste(),price()),"مساحة السطح = π × القطر الخارجي × الطول."){v->val a=PI*v("diameter")/1000*v("length");val buy=a*(1+v("waste")/100);CalcAnswer(listOf(o("صافي سطح",a,"م²"),o("بالقص والهالك",buy,"م²")),"السعر للمتر المربع؛ أدخل قطر العزل الخارجي.",buy*v("price"))}
        addDef("tank","حجم خزان مستطيل",10,listOf(length,width,height,f("freeboard","ارتفاع غير مستخدم","م","0",false)),"الحجم المفيد = الطول × العرض × (الارتفاع − الارتفاع غير المستخدم)."){v->require(v("height")>v("freeboard"));val volume=v("length")*v("width")*(v("height")-v("freeboard"));CalcAnswer(listOf(o("حجم مفيد",volume,"م³"),o("سعة",volume*1000,"لتر")),"الأبعاد الداخلية؛ لا يحدد احتياج المبنى أو تصميم الخزان.")}
        addDef("wires","أسلاك من المسارات والموصلات",11,listOf(length,f("conductors","عدد الموصلات بالمسار","موصل"),f("tail","إجمالي زيادات النهايات","م","0",false),f("roll","طول اللفة","م","100"),waste(),f("price","سعر اللفة","جنيه","0",false)),"طول = طول المسار × الموصلات + زيادات النهايات؛ شراء بالهالك."){v->val net=v("length")*v("conductors")+v("tail");val buy=net*(1+v("waste")/100);val rolls=ceil(buy/v("roll"));CalcAnswer(listOf(o("طول صافي",net,"م"),o("طول مطلوب",buy,"م"),o("لفات",rolls,"لفة")),"حساب منفصل لكل قطاع ولون؛ القطاع محدد مسبقًا.",rolls*v("price"),buy/v("roll")*v("price"))}
        addDef("cables","كابلات ونهايات",11,listOf(length,f("tail","زيادات النهايات والمسارات","م","0",false),waste(),price()),"الشراء = (المسار + الزيادات) × الهالك."){v->val buy=(v("length")+v("tail"))*(1+v("waste")/100);CalcAnswer(listOf(o("طول شراء",buy,"م")),"السعر للمتر؛ نوع الكابل وقطاعه من المواصفات.",buy*v("price"))}
        addDef("conduits","مواسير كهرباء وعلب",11,listOf(length,f("piece","طول الماسورة","م","3"),f("boxes","عدد العلب","عدد","0",false),waste(),f("price","سعر الماسورة","جنيه","0",false),f("boxPrice","سعر العلبة","جنيه","0",false)),"المواسير = طول المسارات بالهالك ÷ طول القطعة؛ العلب بالعدد المدخل."){v->val pieces=ceil(v("length")*(1+v("waste")/100)/v("piece"));CalcAnswer(listOf(o("مواسير",pieces,"ماسورة"),o("علب",v("boxes"),"علبة")),"العلب والوصلات من الحصر الفعلي.",pieces*v("price")+v("boxes")*v("boxPrice"))}
        addDef("trays","حوامل كابلات ودعامات",11,listOf(length,f("piece","طول القطعة","م","3"),f("spacing","أقصى مسافة دعامات","م"),waste(),f("price","سعر قطعة الحامل","جنيه","0",false),f("supportPrice","سعر الدعامة","جنيه","0",false)),"قطع = الطول بالهالك ÷ طول القطعة؛ دعامات = تقريب لأعلى (الطول ÷ المسافة) + 1."){v->val pieces=ceil(v("length")*(1+v("waste")/100)/v("piece"));val supports=ceil(v("length")/v("spacing"))+1;CalcAnswer(listOf(o("قطع حوامل",pieces,"قطعة"),o("دعامات لمسار مستقيم",supports,"عدد")),"الأركان والتفرعات والدعامات الإضافية تُحصر مستقلة.",pieces*v("price")+supports*v("supportPrice"))}
        addDef("points","تسعير نقاط إنارة وبرايز",11,listOf(count,price()),"التكلفة = العدد × سعر النقطة."){v->CalcAnswer(listOf(o("نقاط",v("count"),"نقطة")),"حساب مستقل لكل نوع نقطة؛ لا يشمل أسلاكًا إلا إذا سعر النقطة يشملها.",v("count")*v("price"))}
        addDef("loads","تجميع أحمال أجهزة متماثلة",11,listOf(f("power","قدرة الجهاز المسجلة","واط"),count,f("factor","معامل تشغيل مدخل","معامل","1")),"القدرة = قدرة الجهاز × العدد × معامل التشغيل."){v->CalcAnswer(listOf(o("قدرة مركبة",v("power")*v("count")/1000,"كيلوواط"),o("قدرة بالمعامل",v("power")*v("count")*v("factor")/1000,"كيلوواط")),"تجميع أحمال فقط؛ لا يختار كابلًا أو قاطعًا.")}
    }
    fun forItem(name:String):CalcDef? = all.firstOrNull{it.id==when(name){
        "مباني"->"masonry"
        "الأرضيات","سيراميك الحوائط"->"tile"
        "الوزرات"->"skirting"
        "دهان الحوائط","دهان السقف"->"paint"
        "عزل الأرضية"->"waterproof"
        "سقف جبس بورد"->"gypsum"
        else->""
    }}
    fun defaults(def:CalcDef,project:Map<String,String>)=PriceBook.apply(def,baseDefaults(def,project),project)
    fun baseDefaults(def:CalcDef,project:Map<String,String>):Map<String,String> {
        val result=def.fields.associate{field->field.key to (project["recipe.${def.id}.${field.key}"]?:project[field.key]?.takeIf{field.key in setOf("cementPrice","sandPrice")}?:field.default)}
        val mix=project["recipe.${def.id}._bagsPerSand"]?:project[when(def.id){"splash"->"splashBagsPerSand";"screed"->"screedBagsPerSand";else->"plasterBagsPerSand"}]?.takeIf{def.id in mortarIds}
        val named=result+("_materialName" to project["recipe.${def.id}._materialName"].orEmpty())+listOf("_laborRate","_transportRate","_equipmentRate").mapNotNull{k->project["recipe.${def.id}.$k"]?.let{k to it}}.toMap()
        return if(mix!=null)named+(MortarMix.key to mix)else named
    }
    fun recipe(def:CalcDef,inputs:Map<String,String>,quantity:Double):Map<String,String> = def.fields.associate{it.key to it.default}+inputs+
        ((if(def.fields.any{it.key=="area"})"area" else if(def.fields.any{it.key=="length"})"length" else "count") to quantity.toString())
    val mortarIds=setOf("plaster","splash","screed","bedding","custom_mortar")
    private fun number(raw:String?)=raw.orEmpty().map{if(it.isDigit())it.digitToInt().digitToChar()else it}.joinToString("").replace('٫','.').replace(',','.').toDoubleOrNull()
    fun mortarArea(raw:Map<String,String>):Double {
        val mode=raw["_mortarMode"]?:"area"
        if(mode=="area")return number(raw["area"].orEmpty())?:0.0
        val thickness=number(raw["thickness"].orEmpty())?:0.0
        require(thickness>0){"متوسط السمك يجب أن يكون أكبر من صفر"}
        val volume=when(mode){
            "unit"->1.0
            "sand","stock"->{
                val mixed=MortarMix.normalize(raw);val ratio=MortarMix.bags(mixed)
                val available=number(raw["_sandAvailable"])?:(if(mode=="sand")1.0 else 0.0)
                require(available>0){"أدخل كمية الرمل المتاحة أكبر من صفر"}
                val usable=if(mode=="stock"){
                    val bags=number(raw["_bagsAvailable"])?:0.0
                    require(bags>0){"أدخل شكاير الأسمنت المتاحة أكبر من صفر"}
                    minOf(available,bags/ratio)
                }else available
                val density=number(mixed["density"])?:1440.0;val bag=number(mixed["bag"])?:50.0;val dry=number(mixed["dry"])?:1.33
                require(density>0&&bag>0&&dry>0){"راجع معاملات الخلطة"}
                usable*(1+ratio*bag/density)/dry
            }
            else->number(raw["_volume"].orEmpty())?:0.0
        }
        require(volume>0&&volume.isFinite()){ "حجم المونة يجب أن يكون أكبر من صفر" }
        val waste=number(raw["waste"].orEmpty())?:0.0
        return volume*1000/thickness/(if(mode in setOf("coverage","sand","stock"))1+waste/100 else 1.0)
    }
    fun evaluate(def:CalcDef,raw:Map<String,String>):CalcAnswer {
        if(raw["_areaMethod"]=="dimensions"&&(def.id !in mortarIds||(raw["_mortarMode"]?:"area")=="area"))require((number(raw["_areaLength"].orEmpty())?:0.0)>0&&(number(raw["_areaWidth"].orEmpty())?:0.0)>0){"أدخل طول المسطح وعرضه أكبر من صفر"}
        val mixed=if(MortarMix.isMix(def))MortarMix.normalize(raw)else raw
        val normalized=if(def.id in mortarIds)mixed+("area" to mortarArea(mixed).toString())else mixed
        val values=def.fields.associate{field->
            val text=normalized[field.key].orEmpty().map{if(it.isDigit())it.digitToInt().digitToChar()else it}.joinToString("").replace('٫','.').replace(',','.').replace("٬","")
            val v=if(text.isBlank()&&!field.required)0.0 else text.toDoubleOrNull()
            require(v!=null&&v.isFinite()){ "راجع ${field.label}" }
            require(field.signed||v>=0){"${field.label} لا يمكن أن يكون سالبًا"}
            require(!field.required||v>0){"${field.label} يجب أن يكون أكبر من صفر"}
            if(field.key in setOf("count","steps","layers","sides","faces","coats","pack","conductors","intervals","bags","packs")&&field.unit in setOf("عدد","قطعة","وجه","درجة","طبقة","شبكة","موصل","مسافة","عبوة","شيكارة"))require(v==floor(v)){"${field.label} يجب أن يكون عددًا صحيحًا"}
            field.key to v
        }
        val solved=def.solve(values)
        val answer=when(def.id){
            "gypsum_system"->{val a=values.invoke("area")*(1+values.invoke("waste")/100);solved.copy(consumedCost=a*values.invoke("profiles")/values.invoke("profileL")*values.invoke("profilePrice")+a*values.invoke("hangers")*values.invoke("hangerPrice")+a*values.invoke("screws")*values.invoke("screwPrice"))}
            "conduits"->solved.copy(consumedCost=values.invoke("length")*(1+values.invoke("waste")/100)/values.invoke("piece")*values.invoke("price")+values.invoke("boxes")*values.invoke("boxPrice"))
            "trays"->solved.copy(consumedCost=values.invoke("length")*(1+values.invoke("waste")/100)/values.invoke("piece")*values.invoke("price")+solved.outputs.first{it.label=="دعامات لمسار مستقيم"}.value*values.invoke("supportPrice"))
            else->solved
        }
        require(answer.outputs.all{it.value.isFinite()}&&answer.cost.isFinite()){ "راجع المقاسات والمعدلات؛ نتيجة غير صالحة" }
        if(def.id in mortarIds){
            val area=values["area"]?:0.0
            return answer.copy(outputs=listOf(CalcOutput("مساحة التغطية عند السمك المدخل",area,"م²"))+answer.outputs,
                explanation=MortarMix.summary(raw)+". "+answer.explanation+" القيم وفق الخلطة المدخلة. تغطية الطرطشة تقدير حجمي يُراجع باستهلاك الموقع.")
        }
        return answer
    }
    fun workQuantity(def:CalcDef,raw:Map<String,String>,answer:CalcAnswer):CalcOutput {
        fun output(label:String)=answer.outputs.first{it.label==label}
        return when(def.id){
            "wires"->output("طول صافي")
            "mesh"->{val weight=output("وزن شراء");weight.copy(value=weight.value/(1+(number(raw["waste"])?:0.0)/100))}
            "reverse_mortar"->output("مساحة يمكن تنفيذها")
            "faces"->output("صافي البياض")
            "stairs_finish"->output("صافي الكسوة")
            "points"->output("نقاط").copy(unit="عدد")
            "fill"->output("حجم منفذ")
            "earth_levels"->answer.outputs.first()
            "form_column"->output("مساحة الشدة")
            "pipe_insulation"->output("صافي سطح")
            "cutting"->CalcOutput("قطع مطلوبة",number(raw["count"])?:0.0,"عدد")
            else->when {
                def.id in mortarIds->CalcOutput("صافي التنفيذ",mortarArea(raw),"م²")
                def.fields.any{it.key=="area"}->CalcOutput("صافي التنفيذ",number(raw["area"])?:0.0,"م²")
                answer.outputs.any{it.label=="حجم صافي"}->output("حجم صافي")
                answer.outputs.any{it.label=="مساحة شدة"}->output("مساحة شدة")
                answer.outputs.any{it.label=="وزن"}->output("وزن")
                def.id in setOf("pipes","cables","skirting","skirting_cut","sealant","kerb","conduits","trays")->CalcOutput("صافي التنفيذ",number(raw["length"])?:0.0,"م")
                def.id=="fittings"->CalcOutput("صافي التنفيذ",number(raw["count"])?:0.0,"عدد")
                else->answer.outputs.firstOrNull{it.label.contains("صافي")&&!it.label.contains("فائض")}?:answer.outputs.first()
            }
        }
    }
    fun result(def:CalcDef,raw:Map<String,String>,answer:CalcAnswer)=MaterialResult(def.id,def.title,
        if(def.id in mortarIds)mortarArea(raw)else number(raw["area"].orEmpty())?:number(raw["length"].orEmpty())?:number(raw["count"].orEmpty())?:0.0,
        if("area" in raw)"م²" else if("length" in raw)"م" else if("count" in raw)"عدد" else "",
        answer.outputs.map{MaterialLine(it.label,"${format(it.value)} ${it.unit}")},def.formula+"\n"+answer.explanation,raw,answer.cost)
    private fun format(x:Double)=java.math.BigDecimal.valueOf(x).setScale(3,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

