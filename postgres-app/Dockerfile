FROM postgres:15

# 데이터 경로를 Fly의 볼륨 마운트 경로에 맞게 설정
ENV PGDATA=/data

# 권한 설정
RUN mkdir -p /data && chown -R postgres:postgres /data

USER postgres

# Postgres 실행
CMD ["postgres"]
