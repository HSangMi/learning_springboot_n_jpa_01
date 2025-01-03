## API 개발 기본

* API를 개발할 때, 패키지부터 조금 고민임
* API 스타일의 컨트롤러 패키지와, 템플릿 엔진용 컨트롤러 패키지를 분리하는 편!
  * 공통으로 예외처리 등 작업들이 보통 패키지같은 구성 단위로 공통처리를 하는데
  * api랑 화면은 공통터리를 해야되는 요소가 많이 다른편!
    * ex) 화면은 템플릿 엔진에서 뭔가 문제가 생기면 ,공통 에러화면html이 나와야함.
    * api는 공통에러용 json api 스펙이 나가야함.
  * 이런 관점에따라 미묘한 차이가 있기 때문에 패키지를 분리하는 것을 선호


## 섹션6 - OSIV(Open Session In View)와 성능 최적화  

* JPA에서 EntityManager를 하이버네이트에선 Session으로 불렸음
* Open Session in View(하이버네이트) / Open EntityManager in View(JPA)
* 관례상 OSIV라고함.
* spring boot에 configuration에선 `spring.jpa.open-in-view` 옵션의로 정의함 (디폴트 true)

* jpa가 언제 DB 커넥션을 가져올까?
  * DB 트랜젝션을 시작할 때, jpa의 영속성 컨텍스트가 DB커넥션을 가져옴
* jpa가 언제 DB에 커넥션을 반환할까?
  * OSIV ON인 경우, 트랜젝션이 끝나도, 영속성 컨텍스트를 사용자에게 응답이 반환됐을 때 까지 유지
  * 응답이 완전히 반환되어 더이상 필요가 없을떄 DB커넥션을 반환하고 영속성 컨텍스트에서 사라짐
  * 지연로딩을 하기위해선 영속성 컨텍스트가 살아있어야함!

* OSIV ON 단점!
  * 너무 오랫동안 DB 커넥션 리소스를 사용하기 떄문에, 실시간 트래픽이 중요한 애플리케이션에선 커넥션이 고갈될수 있음
* 장점
  * 엔티티를 적극 활용해서 레이지로딩 같은 기술을 컨트롤러나 뷰에서 적극 활용 가능

* OSIV OFF의 경우 
  * 트랜잭션 범위 == 영속성 컨텍스트 생존 범위 
  * 트랙젝션이 끝나면, 커넥션 flush, commit 다 하고 영속성 컨텍스트도 닫음 => 커넥션 리소스를 낭비하지 않음
* OSIV OFF 단점!
  * 지연로딩을 트랜젝션 안에서 처리해야함, 또는 fetch join 사용 
    * view Template에서 지연로딩 x
* 해결방법 : 커맨드와 쿼리 분리 
  * ex) OrderService : 핵심 비즈니스로직 / OrderQueryService : 화면이나 API에 맞춘 서비스(주로 읽기 전용 트랜잭션)
  * 보통 서비스 계층에서 트랜잭션을 유지하기 떄문에, 두 서비스 모두 트랜잭션을 유지하면서 지연로딩을 사용할 수 있다

* 코드와 유지보수성을 생각하면 OSIV ON이 좋지만, 성능을 생각하면 OSIV OFF가 맞음
* 고객서비스 기반의 트래픽이 많은 실시간 api의 경우 off
* admin 시스템은 그냥 ON해서 씀

## QueryDSL 소개
* QueryDsl추가하기
* build.gradle (버전별 상이함!!!)
  ```build.gradle
  // 라이브러리 추가
    implementation 'com.querydsl:querydsl-jpa'
  implementation 'com.querydsl:querydsl-apt'
    
  // 플러그인 받을 수 있게 dependency 추가
  buildscript {
    dependencies {
      classpath("gradle.plugin.com.ewerk.gradle.plugins:querydsl-plugin:1.0.10")
    }
  }
  // 플러그인 적용
  apply plugin: "com.ewerk.gradle.plugins.querydsl"

  // generated된 Q파일을 어디다 위치할지 셋팅 필요
  def querydslDir = 'src/main/generated'

  querydsl {
    library = "com.querydsl:querydsl-apt"
    jpa = true
    querydslSourcesDir = querydslDir
  }
  sourceSets{
    main {
      java{
        srcDirs = ['src/main.java', querydslDir]
        }
      }
  }
  ```
* gradle > task > other > compileQuerydsl 실행


## 단축키 메모
* `shift + F6` : 변수명 일괄변경
* `ctrl + alt + P` : 메서드에서 파라미터화
* `ctrl + alt + M` : 공통로직 메소드화
* `F2` : 에러 라인으로 이동