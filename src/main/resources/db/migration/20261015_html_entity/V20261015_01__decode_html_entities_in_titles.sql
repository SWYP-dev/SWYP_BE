-- 외부 API 수집 공고의 title/company_name에 남아 있는 HTML 엔티티를 원래 문자로 되돌린다. (#175)
-- &amp;를 먼저 풀어야 이중 escape(&amp;apos;)까지 함께 정리된다. (HtmlEntityUtils.decode와 동일하게 최대 2단계)
-- job_postings/application_postings는 사용자가 직접 입력한 공고(DIRECT)를 제외한다.

UPDATE job_feeds
SET title = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        title, '&amp;', '&'), '&apos;', ''''), '&#39;', ''''), '&quot;', '"'), '&#34;', '"'), '&lt;', '<'), '&gt;', '>')
WHERE title LIKE '%&%;%';

UPDATE job_feeds
SET company_name = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        company_name, '&amp;', '&'), '&apos;', ''''), '&#39;', ''''), '&quot;', '"'), '&#34;', '"'), '&lt;', '<'), '&gt;', '>')
WHERE company_name LIKE '%&%;%';

UPDATE job_postings
SET title = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        title, '&amp;', '&'), '&apos;', ''''), '&#39;', ''''), '&quot;', '"'), '&#34;', '"'), '&lt;', '<'), '&gt;', '>')
WHERE platform <> 'DIRECT'
  AND title LIKE '%&%;%';

UPDATE job_postings
SET company_name = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        company_name, '&amp;', '&'), '&apos;', ''''), '&#39;', ''''), '&quot;', '"'), '&#34;', '"'), '&lt;', '<'), '&gt;', '>')
WHERE platform <> 'DIRECT'
  AND company_name LIKE '%&%;%';

UPDATE application_postings
SET title = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        title, '&amp;', '&'), '&apos;', ''''), '&#39;', ''''), '&quot;', '"'), '&#34;', '"'), '&lt;', '<'), '&gt;', '>')
WHERE platform <> 'DIRECT'
  AND title LIKE '%&%;%';

UPDATE application_postings
SET company_name = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        company_name, '&amp;', '&'), '&apos;', ''''), '&#39;', ''''), '&quot;', '"'), '&#34;', '"'), '&lt;', '<'), '&gt;', '>')
WHERE platform <> 'DIRECT'
  AND company_name LIKE '%&%;%';
